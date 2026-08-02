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
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.Retencao
import com.jean.vocabs.shared.domain.RetencaoAgora
import com.jean.vocabs.shared.domain.ResumoRevisao
import com.jean.vocabs.shared.domain.StatusCaptura
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.shared.domain.UsoIa
import com.jean.vocabs.shared.domain.VocabRepository
import com.jean.vocabs.shared.domain.eValidoEm
import com.jean.vocabs.shared.domain.sequenciaDe
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val removerArquivo: (String) -> Unit = {},
) : VocabRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val queries get() = db.vocabsQueries

    override fun observarProntas(): Flow<List<Entrada>> =
        queries.listarProntas().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }

    override fun observarInbox(): Flow<List<Entrada>> =
        queries.listarInbox().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }

    override fun observarCapturasPendentes(): Flow<List<Captura>> =
        queries.listarCapturasPendentes().asFlow().mapToList(io)
            .map { linhas -> linhas.map(::capturaParaDominio) }

    override fun observarCapturaPorId(id: Long): Flow<Captura?> =
        queries.buscarCapturaPorId(id).asFlow().mapToOneOrNull(io)
            .map { it?.let(::capturaParaDominio) }

    override fun observarPorId(id: Long): Flow<Entrada?> =
        queries.buscarEntradaPorId(id).asFlow().mapToOneOrNull(io).map { it?.let(::paraDominio) }

    override fun observarFilaDeRevisao(): Flow<List<Entrada>> = observarProntas().map { prontas ->
        val instante = agora()
        prontas
            .filter { it.precisaRevisar(instante) }
            .sortedBy { it.retencao?.pontosEm(instante) ?: 0.0 }
    }

    override fun observarResumoDeRevisao(): Flow<ResumoRevisao> = combine(
        observarProntas(),
        queries.listarDiasRevisados().asFlow().mapToList(io),
    ) { prontas, dias ->
        val instante = agora()
        val sequencia = sequenciaDe(dias, diaLocalDe(instante))
        ResumoRevisao(
            naFila = prontas.count { it.precisaRevisar(instante) },
            proximaEmMillis = prontas
                .mapNotNull { it.retencao?.proximaRevisaoEm(instante) }
                .minOrNull(),
            diasSeguidos = sequencia.diasSeguidos,
            revisouHoje = sequencia.revisouHoje,
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
    ): List<Long> = withContext(io) {
        val texto = trecho
        require(texto.isNotBlank()) { "O trecho é obrigatório." }
        require(alvos.isNotEmpty()) { "Selecione ao menos um alvo." }
        require(alvos.all { it.eValidoEm(texto) }) { "Há uma seleção fora do trecho atual." }

        queries.transactionWithResult {
            queries.inserirCaptura(
                trecho = texto,
                origem = null,
                criado_em = agora(),
                status = StatusCaptura.PROCESSADA.name,
                formato = FormatoCaptura.TEXTO.name,
                midia_caminho = null,
                duracao_ms = null,
                erro_transcricao = null,
            )
            val capturaId = queries.ultimoIdInserido().executeAsOne()
            inserirAlvos(capturaId, alvos)
        }
    }

    override suspend fun capturarMidia(
        formato: FormatoCaptura,
        caminho: String,
        duracaoMs: Long?,
    ): Long = withContext(io) {
        require(formato != FormatoCaptura.TEXTO) { "Mídia precisa ser foto ou áudio." }
        queries.transactionWithResult {
            queries.inserirCaptura(
                trecho = null,
                origem = null,
                criado_em = agora(),
                status = StatusCaptura.TRANSCREVENDO.name,
                formato = formato.name,
                midia_caminho = caminho,
                duracao_ms = duracaoMs,
                erro_transcricao = null,
            )
            queries.ultimoIdInserido().executeAsOne()
        }
    }

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
            queries.ultimoIdInserido().executeAsOne()
        }

    override suspend fun gerarFicha(id: Long): Boolean = withContext(io) {
        val linha = queries.buscarEntradaPorId(id).executeAsOneOrNull() ?: return@withContext false
        val trecho = linha.trecho?.takeIf { it.isNotBlank() } ?: return@withContext false
        val alvo = linha.alvo.takeIf { it.isNotBlank() } ?: return@withContext false
        val tipo = tipoDe(linha.tipo)
        val jaEraPronta = StatusEntrada.de(linha.status) == StatusEntrada.PRONTA

        queries.marcarStatus(status = StatusEntrada.GERANDO.name, id = id)
        try {
            val ficha = api.gerar(trecho = trecho, alvo = alvo, tipo = tipo)
            queries.transaction {
                queries.salvarFicha(
                    status = StatusEntrada.PRONTA.name,
                    tipo = tipo.name,
                    traducao = ficha.traducao,
                    definicoes_json = json.encodeToString(ficha.definicoes),
                    exemplo = ficha.exemplo,
                    ipa = ficha.ipa,
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
        val nova = montarRetencao(linha).apos(acertou = acertou, agora = instante)
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
        ipa = linha.ipa.orEmpty(),
        relacionadas = linha.relacionadas_json.listaJson(),
    )

    private fun String?.listaJson(): List<String> = this
        ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
        ?: emptyList()

    private fun tipoDe(valor: String): TipoAlvo =
        runCatching { TipoAlvo.valueOf(valor) }.getOrDefault(TipoAlvo.PALAVRA)
}
