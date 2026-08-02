package com.jean.vocabs.ui.revisao

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface RevisaoEstado {
    data object Carregando : RevisaoEstado
    data object Vazia : RevisaoEstado

    data class Cartao(
        val entrada: Entrada,
        val revelado: Boolean,
        val posicao: Int,
        val total: Int,
    ) : RevisaoEstado

    data class Resumo(
        val acertos: Int,
        val erros: Int,
        val errados: List<String>,
        val diasSeguidos: Int,
        val restantes: Int,
    ) : RevisaoEstado
}

class RevisaoViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)

    private val _estado = MutableStateFlow<RevisaoEstado>(RevisaoEstado.Carregando)
    val estado: StateFlow<RevisaoEstado> = _estado.asStateFlow()

    private var cartas = ArrayDeque<Entrada>()

    /** Quem já foi respondido nesta sessão. Ver [responder]. */
    private val respondidos = mutableSetOf<Long>()

    private var acertos = 0
    private var erros = 0
    private var errados = mutableListOf<String>()
    private var total = 0
    private var restantes = 0

    init {
        novaRodada()
    }

    fun novaRodada() {
        viewModelScope.launch {
            _estado.value = RevisaoEstado.Carregando

            // .first(): um retrato da fila, não uma assinatura contínua. Se a tela
            // coletasse o Flow ao vivo, cada resposta removeria a palavra da fila e
            // a lista se reorganizaria embaixo de você no meio da sessão.
            val fila = repositorio.observarFilaDeRevisao().first()

            restantes = (fila.size - TETO_SESSAO).coerceAtLeast(0)
            acertos = 0
            erros = 0
            errados = mutableListOf()
            respondidos.clear()

            // A fila chega ordenada pelas mais esquecidas primeiro, o que decide
            // QUAIS entram na sessão. O shuffled decide só a ORDEM de apresentação:
            // sem ele você decoraria que o terceiro cartão é sempre a mesma palavra,
            // que é o padrão de tela que o princípio 3 existe para evitar.
            cartas = ArrayDeque(fila.take(TETO_SESSAO).shuffled())
            total = cartas.size

            _estado.value = if (cartas.isEmpty()) RevisaoEstado.Vazia else proximoCartao()
        }
    }

    fun revelar() {
        val atual = _estado.value as? RevisaoEstado.Cartao ?: return
        _estado.value = atual.copy(revelado = true)
    }

    fun responder(acertou: Boolean) {
        val carta = cartas.removeFirstOrNull() ?: return

        // O banco só ouve a PRIMEIRA resposta de cada cartão. A repetição no fim da
        // fila é para você ver a palavra de novo, não para apagar o erro: se ela
        // gravasse, errar e depois acertar sairia com 100 pontos e a taxa apenas
        // multiplicada por 2 em vez de 3 — o erro viraria quase nada.
        if (respondidos.add(carta.id)) {
            if (acertou) {
                acertos++
            } else {
                erros++
                errados += carta.titulo
                // Volta uma vez, no fim da fila. Repetir na hora seria
                // reconhecimento, não recuperação.
                cartas.addLast(carta)
            }
            // Escopo da aplicação: responder e sair na mesma fração de segundo
            // cancelaria a gravação se ela estivesse presa ao viewModelScope.
            AppContainer.escopo.launch { repositorio.registrarResposta(carta.id, acertou) }
        }

        if (cartas.isEmpty()) {
            viewModelScope.launch { _estado.value = resumo() }
        } else {
            _estado.value = proximoCartao()
        }
    }

    private fun proximoCartao(): RevisaoEstado.Cartao {
        val carta = cartas.first()
        return RevisaoEstado.Cartao(
            entrada = carta,
            revelado = false,
            // Limitado ao total porque um cartão errado volta para o fim da fila:
            // sem o teto, a repetição apareceria como "4 de 3".
            posicao = (respondidos.size + 1).coerceAtMost(total),
            total = total,
        )
    }

    private suspend fun resumo(): RevisaoEstado.Resumo {
        val sequencia = repositorio.observarResumoDeRevisao().first()
        return RevisaoEstado.Resumo(
            acertos = acertos,
            erros = erros,
            errados = errados.toList(),
            diasSeguidos = sequencia.diasSeguidos,
            restantes = restantes,
        )
    }

    private companion object {
        /**
         * Na primeira abertura depois da migração, *toda* palavra pronta está na
         * fila — elas foram ancoradas em `criado_em`. Uma sessão de 60 cartões no
         * dia 1 mata o hábito antes de ele começar. 20 dá 4 a 6 minutos, e o que
         * sobra vira "mais uma rodada" no resumo.
         */
        const val TETO_SESSAO = 20
    }
}
