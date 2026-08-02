package com.jean.vocabs.shared.data

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import com.jean.vocabs.contracts.FichaResponse
import com.jean.vocabs.contracts.TipoAlvo
import com.jean.vocabs.shared.data.remote.FichaApi
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.Retencao
import com.jean.vocabs.shared.domain.RetencaoAgora
import com.jean.vocabs.shared.domain.ResumoRevisao
import com.jean.vocabs.shared.domain.StatusEntrada
import com.jean.vocabs.shared.domain.VocabRepository
import com.jean.vocabs.shared.domain.sequenciaDe
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import com.jean.vocabs.shared.db.Entrada as EntradaRow

class VocabRepositoryImpl(
    private val db: VocabsDatabase,
    private val api: FichaApi,
    private val io: CoroutineDispatcher,
    private val agora: () -> Long,
    /**
     * Apagar arquivo é a única coisa aqui que não existe igual em toda
     * plataforma. Fica como lambda pelo mesmo motivo de [agora]: injetar é mais
     * barato que criar um expect/actual para uma linha.
     */
    private val removerArquivo: (String) -> Unit = {},
) : VocabRepository {

    private val json = Json { ignoreUnknownKeys = true }
    private val queries get() = db.vocabsQueries

    override fun observarProntas(): Flow<List<Entrada>> =
        queries.listarProntas().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }

    override fun observarInbox(): Flow<List<Entrada>> =
        queries.listarInbox().asFlow().mapToList(io).map { linhas -> linhas.map(::paraDominio) }

    override fun observarPorId(id: Long): Flow<Entrada?> =
        queries.buscarPorId(id).asFlow().mapToOneOrNull(io).map { it?.let(::paraDominio) }

    override fun observarFilaDeRevisao(): Flow<List<Entrada>> =
        observarProntas().map { prontas ->
            // agora() é chamado AQUI DENTRO, a cada emissão. Se fosse avaliado ao
            // montar o Flow, toda emissão usaria a hora em que o app abriu e a
            // fila congelaria em silêncio — sem erro, sem sintoma óbvio.
            val instante = agora()
            prontas
                .filter { it.precisaRevisar(instante) }
                .sortedBy { it.retencao?.pontosEm(instante) ?: 0.0 }
        }

    override fun observarResumoDeRevisao(): Flow<ResumoRevisao> =
        combine(
            observarProntas(),
            queries.listarDiasRevisados().asFlow().mapToList(io),
        ) { prontas, dias ->
            val instante = agora()
            val hoje = diaLocalDe(instante)
            val sequencia = sequenciaDe(dias, hoje)

            ResumoRevisao(
                naFila = prontas.count { it.precisaRevisar(instante) },
                proximaEmMillis = prontas
                    .mapNotNull { it.retencao?.proximaRevisaoEm(instante) }
                    .minOrNull(),
                diasSeguidos = sequencia.diasSeguidos,
                revisouHoje = sequencia.revisouHoje,
            )
        }

    override fun observarRetencao(id: Long): Flow<RetencaoAgora?> =
        observarPorId(id).map { entrada ->
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

    override suspend fun capturarTexto(trecho: String, alvo: String, origem: String?): Long =
        inserir(
            trecho = trecho.trim(),
            alvo = alvo.trim(),
            origem = origem,
            status = StatusEntrada.PENDENTE,
            formato = FormatoCaptura.TEXTO,
            midiaCaminho = null,
        )

    override suspend fun capturarMidia(
        formato: FormatoCaptura,
        caminho: String,
        origem: String?,
    ): Long = inserir(
        trecho = null,
        alvo = null,
        origem = origem,
        // RASCUNHO e não PENDENTE: não há texto para mandar para a IA ainda, e
        // marcar como pendente faria a geração falhar em loop.
        status = StatusEntrada.RASCUNHO,
        formato = formato,
        midiaCaminho = caminho,
    )

    private suspend fun inserir(
        trecho: String?,
        alvo: String?,
        origem: String?,
        status: StatusEntrada,
        formato: FormatoCaptura,
        midiaCaminho: String?,
    ): Long = withContext(io) {
        queries.transactionWithResult {
            queries.inserirCaptura(
                trecho = trecho,
                alvo = alvo,
                origem = origem?.trim()?.ifBlank { null },
                criado_em = agora(),
                status = status.name,
                formato = formato.name,
                midia_caminho = midiaCaminho,
            )
            queries.ultimoIdInserido().executeAsOne()
        }
    }

    override suspend fun transcrever(id: Long, trecho: String, alvo: String, origem: String?) {
        withContext(io) {
            queries.transcrever(
                trecho = trecho.trim(),
                alvo = alvo.trim(),
                origem = origem?.trim()?.ifBlank { null },
                status = StatusEntrada.PENDENTE.name,
                id = id,
            )
        }
    }

    override suspend fun gerarFicha(id: Long) = withContext(io) {
        val linha = queries.buscarPorId(id).executeAsOneOrNull() ?: return@withContext

        // Rascunho de foto/áudio não tem o que mandar para a IA. Sair em silêncio
        // é melhor que marcar ERRO: não há falha nenhuma, só falta você transcrever.
        val trecho = linha.trecho?.takeIf { it.isNotBlank() } ?: return@withContext
        val alvo = linha.alvo?.takeIf { it.isNotBlank() } ?: return@withContext

        // Regerar uma ficha que já existe não pode zerar a memória da palavra:
        // seriam meses de progresso perdidos por um toque.
        val jaEraPronta = StatusEntrada.de(linha.status) == StatusEntrada.PRONTA

        queries.marcarStatus(status = StatusEntrada.GERANDO.name, id = id)

        try {
            val ficha = api.gerar(trecho = trecho, alvo = alvo)
            queries.transaction {
                queries.salvarFicha(
                    status = StatusEntrada.PRONTA.name,
                    tipo = ficha.tipo.name,
                    traducao = ficha.traducao,
                    definicoes_json = json.encodeToString(ficha.definicoes),
                    exemplo = ficha.exemplo,
                    ipa = ficha.ipa,
                    id = id,
                )
                if (!jaEraPronta) {
                    // A força de memória só começa a contar quando existe ficha
                    // para revisar. Ancorar em criado_em faria uma entrada que
                    // passou dias em ERRO nascer já esquecida.
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
            }
        } catch (cancelamento: CancellationException) {
            // Cancelamento não é falha: se marcasse ERRO aqui, sair da tela no
            // meio da geração deixaria a entrada permanentemente quebrada.
            queries.marcarStatus(status = StatusEntrada.PENDENTE.name, id = id)
            throw cancelamento
        } catch (falha: Exception) {
            queries.marcarErro(
                status = StatusEntrada.ERRO.name,
                erro = falha.message ?: "Falha ao gerar a ficha.",
                id = id,
            )
        }
    }

    override suspend fun registrarResposta(id: Long, acertou: Boolean) = withContext(io) {
        val linha = queries.buscarPorId(id).executeAsOneOrNull() ?: return@withContext
        val instante = agora()

        val nova = montarRetencao(linha).apos(acertou = acertou, agora = instante)

        val dia = diaLocalDe(instante)

        // Uma transação só: o Flow do resumo combina as fichas com os dias
        // revisados, e não pode enxergar um estado escrito pela metade.
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
            // O dia entra a cada resposta, não no fim da sessão: sair no meio não
            // deveria apagar o fato de você ter revisado hoje.
            queries.abrirDia(dia)
            queries.somarRevisao(dia)
        }
    }

    /** A fronteira do dia é decidida pelo SQLite, no fuso do aparelho. */
    private fun diaLocalDe(instante: Long): Long =
        queries.diaLocal(instante).executeAsOne()

    override suspend fun excluir(id: Long) = withContext(io) {
        val linha = queries.buscarPorId(id).executeAsOneOrNull() ?: return@withContext
        queries.excluir(id)
        // Depois do DELETE: se o arquivo sumisse e o banco falhasse, sobraria uma
        // entrada apontando para mídia inexistente.
        linha.midia_caminho?.let(removerArquivo)
    }

    private fun paraDominio(linha: EntradaRow): Entrada {
        val status = StatusEntrada.de(linha.status)
        return Entrada(
            id = linha.id,
            trecho = linha.trecho,
            alvo = linha.alvo,
            origem = linha.origem,
            criadoEm = linha.criado_em,
            status = status,
            formato = FormatoCaptura.de(linha.formato),
            midiaCaminho = linha.midia_caminho,
            ficha = if (status == StatusEntrada.PRONTA) montarFicha(linha) else null,
            // Mesma regra da ficha: retenção existe se e somente se há o que revisar.
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

    private fun montarFicha(linha: EntradaRow) = FichaResponse(
        tipo = runCatching { TipoAlvo.valueOf(linha.tipo.orEmpty()) }.getOrDefault(TipoAlvo.PALAVRA),
        traducao = linha.traducao.orEmpty(),
        definicoes = linha.definicoes_json
            ?.let { runCatching { json.decodeFromString<List<String>>(it) }.getOrNull() }
            ?: emptyList(),
        exemplo = linha.exemplo.orEmpty(),
        ipa = linha.ipa.orEmpty(),
    )
}
