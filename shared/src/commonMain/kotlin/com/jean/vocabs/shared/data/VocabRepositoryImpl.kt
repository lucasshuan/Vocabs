package com.jean.vocabs.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.TargetType
import com.jean.vocabs.shared.data.remote.FichaApi
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.AtividadeDiaria
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.DadosExportacao
import com.jean.vocabs.shared.domain.Degraus
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.Evento
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.MemoryLevel
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.QuotaDoDia
import com.jean.vocabs.shared.domain.Retencao
import com.jean.vocabs.shared.domain.RetencaoAgora
import com.jean.vocabs.shared.domain.ResumoCurso
import com.jean.vocabs.shared.domain.ResumoRevisao
import com.jean.vocabs.shared.domain.CaptureStatus
import com.jean.vocabs.shared.domain.EntryStatus
import com.jean.vocabs.shared.domain.EventType
import com.jean.vocabs.shared.domain.UsoIa
import com.jean.vocabs.shared.domain.VocabRepository
import com.jean.vocabs.shared.domain.eValidoEm
import com.jean.vocabs.shared.domain.melhorSequenciaDe
import com.jean.vocabs.shared.domain.sequenciaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import com.jean.vocabs.shared.db.Capture as CaptureRow
import com.jean.vocabs.shared.db.Entry_with_capture as EntryRow

class VocabRepositoryImpl(
    private val db: VocabsDatabase,
    private val api: FichaApi,
    private val io: CoroutineDispatcher,
    private val agora: () -> Long,
    /**
     * O curso aberto, como fluxo.
     *
     * Entra pelo construtor porque a escolha é uma preferência do aparelho e o
     * repositório é comum aos dois lados do KMP. Ser fluxo (e não um getter) é o
     * que faz todas as telas se refazerem sozinhas quando a pessoa troca de
     * idioma na faixa — sem isso cada ViewModel teria que se reinscrever.
     */
    private val cursoAtivo: Flow<ParIdiomas> = flowOf(ParIdiomas.PADRAO),
    private val removerArquivo: (String) -> Unit = {},
) : VocabRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val queries get() = db.vocabsQueries

    override fun observarCursoAtivo(): Flow<ParIdiomas> = cursoAtivo

    override fun observarCursos(): Flow<List<ResumoCurso>> =
        todasAsProntas().map { prontas ->
            val instante = agora()
            prontas
                .groupBy { it.par }
                .map { (par, entradas) ->
                    ResumoCurso(
                        par = par,
                        total = entradas.size,
                        dominadas = entradas.count { Degraus.nivel(it.degrau) == MemoryLevel.MASTERED },
                        naFila = entradas.count { it.precisaRevisar(instante) },
                        proximaEmMillis = entradas
                            .mapNotNull { it.retencao?.proximaRevisaoEm(instante) }
                            .minOrNull(),
                    )
                }
        }

    private fun todasAsProntas(): Flow<List<Entrada>> =
        queries.listReady().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }

    override fun observarProntas(escopo: Escopo): Flow<List<Entrada>> =
        todasAsProntas().no(escopo) { it.par }

    override fun observarInbox(escopo: Escopo): Flow<List<Entrada>> =
        queries.listInbox().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }
            .no(escopo) { it.par }

    override fun observarCapturasPendentes(escopo: Escopo): Flow<List<Captura>> =
        queries.listPendingCaptures().asFlow().mapToList(io)
            .map { linhas -> linhas.map(::capturaParaDominio) }
            .no(escopo) { it.par }

    /**
     * O recorte de [Escopo] aplicado a uma lista já montada.
     *
     * Filtrar em memória e não em SQL é de propósito: o curso aberto é um fluxo
     * de preferência, e uma consulta parametrizada por ele teria que ser refeita
     * — e o cursor reaberto — a cada troca de idioma na faixa. A lista inteira de
     * fichas de um aparelho cabe folgada na memória; o cursor recriado a cada
     * deslize do carrossel, não.
     */
    private fun <T> Flow<List<T>>.no(escopo: Escopo, par: (T) -> ParIdiomas): Flow<List<T>> =
        combine(cabeNoEscopo(escopo)) { itens, cabe -> itens.filter { cabe(par(it)) } }

    private fun cabeNoEscopo(escopo: Escopo): Flow<(ParIdiomas) -> Boolean> = when (escopo) {
        Escopo.Todos -> flowOf({ _: ParIdiomas -> true })
        is Escopo.Curso -> flowOf({ par: ParIdiomas -> par.alvo == escopo.alvo })
        Escopo.CursoAberto -> cursoAtivo.map { aberto -> { par: ParIdiomas -> par == aberto } }
    }

    override fun observarCapturaPorId(id: Long): Flow<Captura?> =
        queries.findCaptureById(id).asFlow().mapToOneOrNull(io)
            .map { it?.let(::capturaParaDominio) }

    override fun observarPorId(id: Long): Flow<Entrada?> =
        queries.findEntryById(id).asFlow().mapToOneOrNull(io).map { it?.let(::paraDominio) }

    override fun observarEntradas(ids: List<Long>): Flow<List<Entrada>> {
        // `IN ()` não é SQL válido no SQLite, e uma lista vazia é o estado normal
        // da tela de confirmação enquanto o argumento de navegação não chegou.
        if (ids.isEmpty()) return flowOf(emptyList())
        return queries.listEntriesByIds(ids).asFlow().mapToList(io)
            .map { linhas -> linhas.map(::paraDominio) }
    }

    override fun observarFilaDeRevisao(escopo: Escopo): Flow<List<Entrada>> =
        observarProntas(escopo).map { prontas ->
            val instante = agora()
            prontas
                .filter { it.precisaRevisar(instante) }
                .sortedBy { it.retencao?.pontosEm(instante) ?: 0.0 }
        }

    override fun observarResumoDeRevisao(escopo: Escopo): Flow<ResumoRevisao> = combine(
        observarProntas(escopo),
        queries.listReviewedDays().asFlow().mapToList(io),
    ) { prontas, dias ->
        val instante = agora()
        val hoje = diaLocalDe(instante)
        val sequencia = sequenciaDe(dias, hoje)
        val naFila = prontas.count { it.precisaRevisar(instante) }
        ResumoRevisao(
            naFila = naFila,
            proximaEmMillis = prontas
                .mapNotNull { it.retencao?.proximaRevisaoEm(instante) }
                .minOrNull(),
            diasSeguidos = sequencia.diasSeguidos,
            revisouHoje = sequencia.revisouHoje,
            melhorSequencia = melhorSequenciaDe(dias),
            // O que já saiu hoje sai da própria retenção, e não de `dia_revisado`:
            // aquela tabela conta o dia inteiro, de todos os cursos juntos, e a
            // quota é do curso aberto.
            //
            // O corte de 48h antes da conversão não é otimização prematura:
            // `diaLocalDe` é uma consulta ao banco, e sem ele seria uma por
            // palavra a cada emissão do fluxo. Nenhuma revisão de hoje pode estar
            // fora dessa janela, então o corte não muda a resposta.
            quota = QuotaDoDia(
                feita = prontas.count { entrada ->
                    val retencao = entrada.retencao ?: return@count false
                    retencao.revisoes > 0 &&
                        instante - retencao.ultimaInteracao < DOIS_DIAS_EM_MILLIS &&
                        diaLocalDe(retencao.ultimaInteracao) == hoje
                },
                naFila = naFila,
            ),
        )
    }

    override fun observarRetencao(id: Long): Flow<RetencaoAgora?> = observarPorId(id).map { entrada ->
        val retencao = entrada?.retencao ?: return@map null
        val instante = agora()
        RetencaoAgora(
            pontos = retencao.pontosEm(instante),
            nivel = retencao.nivelEm(instante),
            proximaEmMillis = retencao.proximaRevisaoEm(instante),
            revisoes = retencao.revisoes,
            acertos = retencao.acertos,
            erros = retencao.erros,
        )
    }

    override fun observarAtividade(dias: Int): Flow<List<AtividadeDiaria>> {
        val primeiroDia = diaLocalDe(agora()) - (dias.coerceAtLeast(1) - 1)
        return queries.listActivitySince(primeiroDia) { dia, revisoes ->
            AtividadeDiaria(dia = dia, revisoes = revisoes.toInt())
        }.asFlow().mapToList(io)
    }

    override fun observarEventos(dias: Int, escopo: Escopo): Flow<List<Evento>> {
        val primeiroDia = diaLocalDe(agora()) - (dias.coerceAtLeast(1) - 1)
        return queries.listEventsSince(primeiroDia) { id, entradaId, dia, instante, tipo, detalhe, alvo, nativo, alvoIdioma ->
            Evento(
                id = id,
                entradaId = entradaId,
                dia = dia,
                instante = instante,
                tipo = EventType.de(tipo),
                alvo = alvo,
                par = ParIdiomas(nativo = nativo, alvo = alvoIdioma),
                detalhe = detalhe,
            )
        }.asFlow().mapToList(io).no(escopo) { it.par }
    }

    override fun observarUsoIa(): Flow<UsoIa> {
        val mes = mesLocalDe(agora())
        return queries.observeAiUsageOfMonth(mes).asFlow().mapToOneOrNull(io).map { geracoes ->
            UsoIa(mes = mes, usadas = geracoes?.toInt() ?: 0)
        }
    }

    override suspend fun dadosParaExportacao(): DadosExportacao = withContext(io) {
        val mes = mesLocalDe(agora())
        queries.transactionWithResult {
            DadosExportacao(
                capturas = queries.listAllCaptures().executeAsList().map(::capturaParaDominio),
                entradas = queries.listAllEntries().executeAsList().map(::paraDominio),
                atividade = queries.listActivitySince(Long.MIN_VALUE) { dia, revisoes ->
                    AtividadeDiaria(dia, revisoes.toInt())
                }.executeAsList(),
                usoIa = UsoIa(
                    mes = mes,
                    usadas = queries.observeAiUsageOfMonth(mes).executeAsOneOrNull()?.toInt() ?: 0,
                ),
            )
        }
    }

    override suspend fun capturarTexto(
        trecho: String,
        alvos: List<AlvoSelecionado>,
        par: ParIdiomas?,
    ): List<Long> = withContext(io) {
        val texto = trecho
        require(texto.isNotBlank()) { "O trecho é obrigatório." }
        require(alvos.isNotEmpty()) { "Selecione ao menos um alvo." }
        require(alvos.all { it.eValidoEm(texto) }) { "Há uma seleção fora do trecho atual." }

        val curso = cursoDaCaptura(par)
        queries.transactionWithResult {
            val capturaId = inserirCaptura(
                trecho = texto,
                status = CaptureStatus.PROCESSED,
                formato = CaptureFormat.TEXT,
                curso = curso,
            )
            inserirAlvos(capturaId, alvos)
        }
    }

    override suspend fun capturarTrecho(trecho: String, par: ParIdiomas?): Long = withContext(io) {
        require(trecho.isNotBlank()) { "O trecho é obrigatório." }
        val curso = cursoDaCaptura(par)
        queries.transactionWithResult {
            inserirCaptura(
                trecho = trecho,
                status = CaptureStatus.AWAITING_SELECTION,
                formato = CaptureFormat.TEXT,
                curso = curso,
            )
        }
    }

    override suspend fun capturarMidia(
        formato: CaptureFormat,
        caminho: String,
        duracaoMs: Long?,
        par: ParIdiomas?,
    ): Long = withContext(io) {
        require(formato != CaptureFormat.TEXT) { "Mídia precisa ser foto ou áudio." }
        val curso = cursoDaCaptura(par)
        queries.transactionWithResult {
            inserirCaptura(
                trecho = null,
                status = CaptureStatus.TRANSCRIBING,
                formato = formato,
                curso = curso,
                caminho = caminho,
                duracaoMs = duracaoMs,
            )
        }
    }

    override suspend fun alterarIdiomaDaCaptura(id: Long, alvo: String): Unit = withContext(io) {
        queries.changeCaptureLanguage(target_language = alvo, id = id)
    }

    private fun inserirCaptura(
        trecho: String?,
        status: CaptureStatus,
        formato: CaptureFormat,
        curso: ParIdiomas,
        caminho: String? = null,
        duracaoMs: Long? = null,
    ): Long {
        queries.insertCapture(
            snippet = trecho,
            source = null,
            created_at = agora(),
            status = status.name,
            format = formato.name,
            media_path = caminho,
            duration_ms = duracaoMs,
            transcription_error = null,
            native_language = curso.nativo,
            target_language = curso.alvo,
        )
        return queries.lastInsertedId().executeAsOne()
    }

    /**
     * Em que par esta captura nasce — o escolhido na folha, ou o curso aberto.
     *
     * Desde que o idioma passou a ser decidido no ato da gravação, o curso aberto
     * virou só o palpite inicial: capturar em espanhol estando na página do
     * inglês é um caso normal, e o que chega aqui é a decisão já tomada.
     */
    private suspend fun cursoDaCaptura(par: ParIdiomas?): ParIdiomas = par ?: cursoAtivo.first()

    override suspend fun registrarTranscricao(id: Long, trecho: String?, erro: String?) {
        withContext(io) {
            queries.recordTranscription(
                snippet = trecho?.trim()?.ifBlank { null },
                transcription_error = erro?.trim()?.ifBlank { null },
                id = id,
            )
        }
    }

    override suspend fun confirmarCaptura(
        id: Long,
        trecho: String,
        alvos: List<AlvoSelecionado>,
    ): List<Long> = withContext(io) {
        val texto = trecho
        require(texto.isNotBlank()) { "O trecho é obrigatório." }
        require(alvos.isNotEmpty()) { "Selecione ao menos um alvo." }
        require(alvos.all { it.eValidoEm(texto) }) { "Há uma seleção fora do trecho atual." }

        queries.transactionWithResult {
            val existentes = queries.listIdsOfCapture(id).executeAsList()
            if (existentes.isNotEmpty()) return@transactionWithResult existentes
            queries.processCapture(snippet = texto, id = id)
            inserirAlvos(id, alvos)
        }
    }

    private fun inserirAlvos(capturaId: Long, alvos: List<AlvoSelecionado>): List<Long> =
        alvos.distinctBy { it.inicio to it.fim }.map { alvo ->
            queries.insertEntry(
                capture_id = capturaId,
                target = alvo.texto.trim(),
                start_index = alvo.inicio.toLong(),
                end_index = alvo.fim.toLong(),
                type = alvo.tipo.name,
                status = EntryStatus.PENDING.name,
            )
            val id = queries.lastInsertedId().executeAsOne()
            anotar(id, EventType.CAPTURED)
            id
        }

    /**
     * Uma linha na linha do tempo. Sempre dentro da transação de quem chamou —
     * um evento sem o fato que ele descreve seria pior que evento nenhum.
     */
    private fun anotar(entradaId: Long, tipo: EventType, detalhe: String? = null) {
        val instante = agora()
        queries.recordEvent(
            entry_id = entradaId,
            day = diaLocalDe(instante),
            occurred_at = instante,
            type = tipo.name,
            detail = detalhe,
        )
    }

    override suspend fun gerarFicha(id: Long): Boolean = withContext(io) {
        val linha = queries.findEntryById(id).executeAsOneOrNull() ?: return@withContext false
        val trecho = linha.snippet?.takeIf { it.isNotBlank() } ?: return@withContext false
        val alvo = linha.target.takeIf { it.isNotBlank() } ?: return@withContext false
        val tipo = tipoDe(linha.type)
        val jaEraPronta = EntryStatus.de(linha.status) == EntryStatus.READY

        queries.markStatus(status = EntryStatus.GENERATING.name, id = id)
        try {
            val ficha = api.gerar(
                trecho = trecho,
                alvo = alvo,
                tipo = tipo,
                par = ParIdiomas(nativo = linha.native_language, alvo = linha.target_language),
            )
            queries.transaction {
                queries.saveCard(
                    status = EntryStatus.READY.name,
                    type = tipo.name,
                    translation = ficha.traducao,
                    definitions_json = json.encodeToString(ficha.definicoes),
                    example = ficha.exemplo,
                    pronunciation = ficha.pronuncia,
                    related_json = json.encodeToString(ficha.relacionadas),
                    id = id,
                )
                if (!jaEraPronta) {
                    val inicial = Retencao.inicial(agora())
                    queries.saveRetention(
                        points = inicial.pontos,
                        decay_rate = inicial.taxa,
                        last_interaction_at = inicial.ultimaInteracao,
                        reviews = inicial.revisoes.toLong(),
                        correct_count = inicial.acertos.toLong(),
                        incorrect_count = inicial.erros.toLong(),
                        id = id,
                    )
                    // Só na primeira vez: regerar uma ficha que já existia não é
                    // um acontecimento do dia, é conserto.
                    anotar(id, EventType.CARD_READY)
                }
                val mes = mesLocalDe(agora())
                queries.openAiMonth(mes)
                queries.addAiGeneration(mes)
            }
            true
        } catch (cancelamento: CancellationException) {
            queries.markStatus(status = EntryStatus.PENDING.name, id = id)
            throw cancelamento
        } catch (falha: Exception) {
            queries.markError(
                status = EntryStatus.ERROR.name,
                // error_code stays null until the server sends one; the message
                // is still free text at this point.
                error_code = null,
                error_detail = falha.message ?: "Falha ao gerar a ficha.",
                id = id,
            )
            false
        }
    }

    override suspend fun gerarFichas(ids: List<Long>, concorrencia: Int): List<Boolean> =
        coroutineScope {
            val limite = Semaphore(concorrencia.coerceAtLeast(1))
            ids.map { id -> async { limite.withPermit { gerarFicha(id) } } }.awaitAll()
        }

    override suspend fun registrarResposta(id: Long, acertou: Boolean) = withContext(io) {
        val linha = queries.findEntryById(id).executeAsOneOrNull() ?: return@withContext
        val instante = agora()
        val anterior = montarRetencao(linha)
        val nova = anterior.apos(acertou = acertou, agora = instante)
        val dia = diaLocalDe(instante)

        queries.transaction {
            queries.saveRetention(
                points = nova.pontos,
                decay_rate = nova.taxa,
                last_interaction_at = nova.ultimaInteracao,
                reviews = nova.revisoes.toLong(),
                correct_count = nova.acertos.toLong(),
                incorrect_count = nova.erros.toLong(),
                id = id,
            )
            queries.openDay(dia)
            queries.addReview(dia)

            anotar(
                entradaId = id,
                tipo = if (acertou) EventType.CORRECT else EventType.INCORRECT,
                detalhe = nova.revisoes.toString(),
            )
            // A mudança de nível é o que a linha do tempo chama de "virou
            // dominada", e ela só existe comparando antes e depois — depois de
            // gravado, o antes some.
            val subiu = Degraus.nivel(Degraus.de(nova))
            if (subiu != Degraus.nivel(Degraus.de(anterior))) {
                anotar(entradaId = id, tipo = EventType.LEVELED_UP, detalhe = subiu.name)
            }
        }
    }

    override suspend fun excluir(id: Long) = withContext(io) {
        val caminho = queries.transactionWithResult {
            val linha = queries.findEntryById(id).executeAsOneOrNull()
                ?: return@transactionWithResult null
            queries.deleteEntry(id)
            if (queries.countEntriesOfCapture(linha.capture_id).executeAsOne() == 0L) {
                queries.deleteCapture(linha.capture_id)
                linha.media_path
            } else {
                null
            }
        }
        caminho?.let(removerArquivo)
        Unit
    }

    override suspend fun excluirCaptura(id: Long) = withContext(io) {
        val caminho = queries.findCaptureById(id).executeAsOneOrNull()?.media_path
        queries.transaction {
            queries.deleteEntriesOfCapture(id)
            queries.deleteCapture(id)
        }
        caminho?.let(removerArquivo)
        Unit
    }

    private fun diaLocalDe(instante: Long): Long = queries.localDay(instante).executeAsOne()

    private fun mesLocalDe(instante: Long): String = queries.localMonth(instante).executeAsOne()

    private fun capturaParaDominio(linha: CaptureRow) = Captura(
        id = linha.id,
        trecho = linha.snippet,
        origem = linha.source,
        criadoEm = linha.created_at,
        status = CaptureStatus.de(linha.status),
        formato = CaptureFormat.de(linha.format),
        midiaCaminho = linha.media_path,
        duracaoMs = linha.duration_ms,
        erroTranscricao = linha.transcription_error,
        par = ParIdiomas(nativo = linha.native_language, alvo = linha.target_language),
    )

    private fun paraDominio(linha: EntryRow): Entrada {
        val status = EntryStatus.de(linha.status)
        val tipo = tipoDe(linha.type)
        return Entrada(
            id = linha.id,
            capturaId = linha.capture_id,
            trecho = linha.snippet,
            alvo = linha.target,
            inicio = linha.start_index?.toInt(),
            fim = linha.end_index?.toInt(),
            tipo = tipo,
            origem = linha.source,
            criadoEm = linha.created_at,
            status = status,
            formato = CaptureFormat.de(linha.format),
            midiaCaminho = linha.media_path,
            ficha = if (status == EntryStatus.READY) montarFicha(linha, tipo) else null,
            retencao = if (status == EntryStatus.READY) montarRetencao(linha) else null,
            erro = linha.error_detail,
            par = ParIdiomas(nativo = linha.native_language, alvo = linha.target_language),
        )
    }

    private fun montarRetencao(linha: EntryRow) = Retencao(
        pontos = linha.points,
        taxa = linha.decay_rate,
        ultimaInteracao = linha.last_interaction_at,
        revisoes = linha.reviews.toInt(),
        acertos = linha.correct_count.toInt(),
        erros = linha.incorrect_count.toInt(),
    )

    private fun montarFicha(linha: EntryRow, tipo: TargetType) = FichaResponse(
        tipo = tipo,
        traducao = linha.translation.orEmpty(),
        definicoes = linha.definitions_json.listaJson(),
        exemplo = linha.example.orEmpty(),
        pronuncia = linha.pronunciation.orEmpty(),
        relacionadas = linha.related_json.listaJson(),
    )

    private fun String?.listaJson(): List<String> = this
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        ?: emptyList()

    private fun tipoDe(valor: String): TargetType =
        runCatching { TargetType.valueOf(valor) }.getOrDefault(TargetType.WORD)

    private companion object {
        const val DOIS_DIAS_EM_MILLIS = 2 * 86_400_000L
    }
}
