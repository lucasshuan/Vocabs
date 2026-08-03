package com.jean.vocabs.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.TipoAlvo
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
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.NivelMemoria
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.QuotaDoDia
import com.jean.vocabs.shared.domain.Retencao
import com.jean.vocabs.shared.domain.RetencaoAgora
import com.jean.vocabs.shared.domain.ResumoCurso
import com.jean.vocabs.shared.domain.ResumoRevisao
import com.jean.vocabs.shared.domain.StatusCaptura
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.shared.domain.TipoEvento
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
import com.jean.vocabs.shared.db.Captura as CapturaRow
import com.jean.vocabs.shared.db.Entrada_com_captura as EntradaRow

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
                        dominadas = entradas.count { Degraus.nivel(it.degrau) == NivelMemoria.DOMINADA },
                        naFila = entradas.count { it.precisaRevisar(instante) },
                        proximaEmMillis = entradas
                            .mapNotNull { it.retencao?.proximaRevisaoEm(instante) }
                            .minOrNull(),
                    )
                }
        }

    private fun todasAsProntas(): Flow<List<Entrada>> =
        queries.listarProntas().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }

    override fun observarProntas(escopo: Escopo): Flow<List<Entrada>> =
        todasAsProntas().no(escopo) { it.par }

    override fun observarInbox(escopo: Escopo): Flow<List<Entrada>> =
        queries.listarInbox().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }
            .no(escopo) { it.par }

    override fun observarCapturasPendentes(escopo: Escopo): Flow<List<Captura>> =
        queries.listarCapturasPendentes().asFlow().mapToList(io)
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
        queries.buscarCapturaPorId(id).asFlow().mapToOneOrNull(io)
            .map { it?.let(::capturaParaDominio) }

    override fun observarPorId(id: Long): Flow<Entrada?> =
        queries.buscarEntradaPorId(id).asFlow().mapToOneOrNull(io).map { it?.let(::paraDominio) }

    override fun observarEntradas(ids: List<Long>): Flow<List<Entrada>> {
        // `IN ()` não é SQL válido no SQLite, e uma lista vazia é o estado normal
        // da tela de confirmação enquanto o argumento de navegação não chegou.
        if (ids.isEmpty()) return flowOf(emptyList())
        return queries.listarEntradasPorIds(ids).asFlow().mapToList(io)
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
        queries.listarDiasRevisados().asFlow().mapToList(io),
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
        return queries.listarAtividadeDesde(primeiroDia) { dia, revisoes ->
            AtividadeDiaria(dia = dia, revisoes = revisoes.toInt())
        }.asFlow().mapToList(io)
    }

    override fun observarEventos(dias: Int, escopo: Escopo): Flow<List<Evento>> {
        val primeiroDia = diaLocalDe(agora()) - (dias.coerceAtLeast(1) - 1)
        return queries.listarEventosDesde(primeiroDia) { id, entradaId, dia, instante, tipo, detalhe, alvo, nativo, alvoIdioma ->
            Evento(
                id = id,
                entradaId = entradaId,
                dia = dia,
                instante = instante,
                tipo = TipoEvento.de(tipo),
                alvo = alvo,
                par = ParIdiomas(nativo = nativo, alvo = alvoIdioma),
                detalhe = detalhe,
            )
        }.asFlow().mapToList(io).no(escopo) { it.par }
    }

    override fun observarUsoIa(): Flow<UsoIa> {
        val mes = mesLocalDe(agora())
        return queries.observarUsoIaDoMes(mes).asFlow().mapToOneOrNull(io).map { geracoes ->
            UsoIa(mes = mes, usadas = geracoes?.toInt() ?: 0)
        }
    }

    override suspend fun dadosParaExportacao(): DadosExportacao = withContext(io) {
        val mes = mesLocalDe(agora())
        queries.transactionWithResult {
            DadosExportacao(
                capturas = queries.listarTodasCapturas().executeAsList().map(::capturaParaDominio),
                entradas = queries.listarTodasEntradas().executeAsList().map(::paraDominio),
                atividade = queries.listarAtividadeDesde(Long.MIN_VALUE) { dia, revisoes ->
                    AtividadeDiaria(dia, revisoes.toInt())
                }.executeAsList(),
                usoIa = UsoIa(
                    mes = mes,
                    usadas = queries.observarUsoIaDoMes(mes).executeAsOneOrNull()?.toInt() ?: 0,
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
                status = StatusCaptura.PROCESSADA,
                formato = FormatoCaptura.TEXTO,
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
                status = StatusCaptura.AGUARDANDO_SELECAO,
                formato = FormatoCaptura.TEXTO,
                curso = curso,
            )
        }
    }

    override suspend fun capturarMidia(
        formato: FormatoCaptura,
        caminho: String,
        duracaoMs: Long?,
        par: ParIdiomas?,
    ): Long = withContext(io) {
        require(formato != FormatoCaptura.TEXTO) { "Mídia precisa ser foto ou áudio." }
        val curso = cursoDaCaptura(par)
        queries.transactionWithResult {
            inserirCaptura(
                trecho = null,
                status = StatusCaptura.TRANSCREVENDO,
                formato = formato,
                curso = curso,
                caminho = caminho,
                duracaoMs = duracaoMs,
            )
        }
    }

    override suspend fun alterarIdiomaDaCaptura(id: Long, alvo: String): Unit = withContext(io) {
        queries.alterarIdiomaDaCaptura(idioma_alvo = alvo, id = id)
    }

    private fun inserirCaptura(
        trecho: String?,
        status: StatusCaptura,
        formato: FormatoCaptura,
        curso: ParIdiomas,
        caminho: String? = null,
        duracaoMs: Long? = null,
    ): Long {
        queries.inserirCaptura(
            trecho = trecho,
            origem = null,
            criado_em = agora(),
            status = status.name,
            formato = formato.name,
            midia_caminho = caminho,
            duracao_ms = duracaoMs,
            erro_transcricao = null,
            idioma_nativo = curso.nativo,
            idioma_alvo = curso.alvo,
        )
        return queries.ultimoIdInserido().executeAsOne()
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
            queries.registrarTranscricao(
                trecho = trecho?.trim()?.ifBlank { null },
                erro_transcricao = erro?.trim()?.ifBlank { null },
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
            val existentes = queries.listarIdsDaCaptura(id).executeAsList()
            if (existentes.isNotEmpty()) return@transactionWithResult existentes
            queries.processarCaptura(trecho = texto, id = id)
            inserirAlvos(id, alvos)
        }
    }

    private fun inserirAlvos(capturaId: Long, alvos: List<AlvoSelecionado>): List<Long> =
        alvos.distinctBy { it.inicio to it.fim }.map { alvo ->
            queries.inserirEntrada(
                captura_id = capturaId,
                alvo = alvo.texto.trim(),
                inicio = alvo.inicio.toLong(),
                fim = alvo.fim.toLong(),
                tipo = alvo.tipo.name,
                status = StatusEntrada.PENDENTE.name,
            )
            val id = queries.ultimoIdInserido().executeAsOne()
            anotar(id, TipoEvento.CAPTURADA)
            id
        }

    /**
     * Uma linha na linha do tempo. Sempre dentro da transação de quem chamou —
     * um evento sem o fato que ele descreve seria pior que evento nenhum.
     */
    private fun anotar(entradaId: Long, tipo: TipoEvento, detalhe: String? = null) {
        val instante = agora()
        queries.registrarEvento(
            entrada_id = entradaId,
            dia = diaLocalDe(instante),
            instante = instante,
            tipo = tipo.name,
            detalhe = detalhe,
        )
    }

    override suspend fun gerarFicha(id: Long): Boolean = withContext(io) {
        val linha = queries.buscarEntradaPorId(id).executeAsOneOrNull() ?: return@withContext false
        val trecho = linha.trecho?.takeIf { it.isNotBlank() } ?: return@withContext false
        val alvo = linha.alvo.takeIf { it.isNotBlank() } ?: return@withContext false
        val tipo = tipoDe(linha.tipo)
        val jaEraPronta = StatusEntrada.de(linha.status) == StatusEntrada.PRONTA

        queries.marcarStatus(status = StatusEntrada.GERANDO.name, id = id)
        try {
            val ficha = api.gerar(
                trecho = trecho,
                alvo = alvo,
                tipo = tipo,
                par = ParIdiomas(nativo = linha.idioma_nativo, alvo = linha.idioma_alvo),
            )
            queries.transaction {
                queries.salvarFicha(
                    status = StatusEntrada.PRONTA.name,
                    tipo = tipo.name,
                    traducao = ficha.traducao,
                    definicoes_json = json.encodeToString(ficha.definicoes),
                    exemplo = ficha.exemplo,
                    pronuncia = ficha.pronuncia,
                    relacionadas_json = json.encodeToString(ficha.relacionadas),
                    id = id,
                )
                if (!jaEraPronta) {
                    val inicial = Retencao.inicial(agora())
                    queries.salvarRetencao(
                        pontos = inicial.pontos,
                        taxa_decaimento = inicial.taxa,
                        data_ultima_interacao = inicial.ultimaInteracao,
                        revisoes = inicial.revisoes.toLong(),
                        acertos = inicial.acertos.toLong(),
                        erros = inicial.erros.toLong(),
                        id = id,
                    )
                    // Só na primeira vez: regerar uma ficha que já existia não é
                    // um acontecimento do dia, é conserto.
                    anotar(id, TipoEvento.FICHA_PRONTA)
                }
                val mes = mesLocalDe(agora())
                queries.abrirMesIa(mes)
                queries.somarGeracaoIa(mes)
            }
            true
        } catch (cancelamento: CancellationException) {
            queries.marcarStatus(status = StatusEntrada.PENDENTE.name, id = id)
            throw cancelamento
        } catch (falha: Exception) {
            queries.marcarErro(
                status = StatusEntrada.ERRO.name,
                erro = falha.message ?: "Falha ao gerar a ficha.",
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
        val linha = queries.buscarEntradaPorId(id).executeAsOneOrNull() ?: return@withContext
        val instante = agora()
        val anterior = montarRetencao(linha)
        val nova = anterior.apos(acertou = acertou, agora = instante)
        val dia = diaLocalDe(instante)

        queries.transaction {
            queries.salvarRetencao(
                pontos = nova.pontos,
                taxa_decaimento = nova.taxa,
                data_ultima_interacao = nova.ultimaInteracao,
                revisoes = nova.revisoes.toLong(),
                acertos = nova.acertos.toLong(),
                erros = nova.erros.toLong(),
                id = id,
            )
            queries.abrirDia(dia)
            queries.somarRevisao(dia)

            anotar(
                entradaId = id,
                tipo = if (acertou) TipoEvento.ACERTO else TipoEvento.ERRO,
                detalhe = nova.revisoes.toString(),
            )
            // A mudança de nível é o que a linha do tempo chama de "virou
            // dominada", e ela só existe comparando antes e depois — depois de
            // gravado, o antes some.
            val subiu = Degraus.nivel(Degraus.de(nova))
            if (subiu != Degraus.nivel(Degraus.de(anterior))) {
                anotar(entradaId = id, tipo = TipoEvento.SUBIU_NIVEL, detalhe = subiu.name)
            }
        }
    }

    override suspend fun excluir(id: Long) = withContext(io) {
        val caminho = queries.transactionWithResult {
            val linha = queries.buscarEntradaPorId(id).executeAsOneOrNull()
                ?: return@transactionWithResult null
            queries.excluirEntrada(id)
            if (queries.contarEntradasDaCaptura(linha.captura_id).executeAsOne() == 0L) {
                queries.excluirCaptura(linha.captura_id)
                linha.midia_caminho
            } else {
                null
            }
        }
        caminho?.let(removerArquivo)
        Unit
    }

    override suspend fun excluirCaptura(id: Long) = withContext(io) {
        val caminho = queries.buscarCapturaPorId(id).executeAsOneOrNull()?.midia_caminho
        queries.transaction {
            queries.excluirEntradasDaCaptura(id)
            queries.excluirCaptura(id)
        }
        caminho?.let(removerArquivo)
        Unit
    }

    private fun diaLocalDe(instante: Long): Long = queries.diaLocal(instante).executeAsOne()

    private fun mesLocalDe(instante: Long): String = queries.mesLocal(instante).executeAsOne()

    private fun capturaParaDominio(linha: CapturaRow) = Captura(
        id = linha.id,
        trecho = linha.trecho,
        origem = linha.origem,
        criadoEm = linha.criado_em,
        status = StatusCaptura.de(linha.status),
        formato = FormatoCaptura.de(linha.formato),
        midiaCaminho = linha.midia_caminho,
        duracaoMs = linha.duracao_ms,
        erroTranscricao = linha.erro_transcricao,
        par = ParIdiomas(nativo = linha.idioma_nativo, alvo = linha.idioma_alvo),
    )

    private fun paraDominio(linha: EntradaRow): Entrada {
        val status = StatusEntrada.de(linha.status)
        val tipo = tipoDe(linha.tipo)
        return Entrada(
            id = linha.id,
            capturaId = linha.captura_id,
            trecho = linha.trecho,
            alvo = linha.alvo,
            inicio = linha.inicio?.toInt(),
            fim = linha.fim?.toInt(),
            tipo = tipo,
            origem = linha.origem,
            criadoEm = linha.criado_em,
            status = status,
            formato = FormatoCaptura.de(linha.formato),
            midiaCaminho = linha.midia_caminho,
            ficha = if (status == StatusEntrada.PRONTA) montarFicha(linha, tipo) else null,
            retencao = if (status == StatusEntrada.PRONTA) montarRetencao(linha) else null,
            erro = linha.erro,
            par = ParIdiomas(nativo = linha.idioma_nativo, alvo = linha.idioma_alvo),
        )
    }

    private fun montarRetencao(linha: EntradaRow) = Retencao(
        pontos = linha.pontos,
        taxa = linha.taxa_decaimento,
        ultimaInteracao = linha.data_ultima_interacao,
        revisoes = linha.revisoes.toInt(),
        acertos = linha.acertos.toInt(),
        erros = linha.erros.toInt(),
    )

    private fun montarFicha(linha: EntradaRow, tipo: TipoAlvo) = FichaResponse(
        tipo = tipo,
        traducao = linha.traducao.orEmpty(),
        definicoes = linha.definicoes_json.listaJson(),
        exemplo = linha.exemplo.orEmpty(),
        pronuncia = linha.pronuncia.orEmpty(),
        relacionadas = linha.relacionadas_json.listaJson(),
    )

    private fun String?.listaJson(): List<String> = this
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        ?: emptyList()

    private fun tipoDe(valor: String): TipoAlvo =
        runCatching { TipoAlvo.valueOf(valor) }.getOrDefault(TipoAlvo.PALAVRA)

    private companion object {
        const val DOIS_DIAS_EM_MILLIS = 2 * 86_400_000L
    }
}
