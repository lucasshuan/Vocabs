package com.jean.vocabs.ui.revisao

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.respostaCorreta
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class FeedbackRevisao { CORRETA, INCORRETA, NAO_LEMBRO }

sealed interface RevisaoEstado {
    data object Carregando : RevisaoEstado
    data object Vazia : RevisaoEstado
    data class Cartao(
        val entrada: Entrada,
        val resposta: String = "",
        val feedback: FeedbackRevisao? = null,
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
    private val respondidos = mutableSetOf<Long>()
    private val recolocados = mutableSetOf<Long>()
    private var acertos = 0
    private var erros = 0
    private var errados = mutableListOf<String>()
    private var total = 0
    private var restantes = 0

    init { novaRodada() }

    fun novaRodada() {
        viewModelScope.launch {
            _estado.value = RevisaoEstado.Carregando
            val fila = repositorio.observarFilaDeRevisao().first()
            restantes = (fila.size - TETO_SESSAO).coerceAtLeast(0)
            acertos = 0
            erros = 0
            errados.clear()
            respondidos.clear()
            recolocados.clear()
            cartas = ArrayDeque(fila.take(TETO_SESSAO).shuffled())
            total = cartas.size
            _estado.value = if (cartas.isEmpty()) RevisaoEstado.Vazia else proximoCartao()
        }
    }

    fun editarResposta(valor: String) {
        val atual = _estado.value as? RevisaoEstado.Cartao ?: return
        if (atual.feedback == null) _estado.value = atual.copy(resposta = valor)
    }

    fun confirmar() {
        val atual = _estado.value as? RevisaoEstado.Cartao ?: return
        if (atual.feedback != null || atual.resposta.isBlank()) return
        avaliar(respostaCorreta(atual.resposta, atual.entrada.alvo.orEmpty()), naoLembrou = false)
    }

    fun naoLembro() {
        val atual = _estado.value as? RevisaoEstado.Cartao ?: return
        if (atual.feedback == null) avaliar(acertou = false, naoLembrou = true)
    }

    private fun avaliar(acertou: Boolean, naoLembrou: Boolean) {
        val atual = _estado.value as? RevisaoEstado.Cartao ?: return
        if (respondidos.add(atual.entrada.id)) {
            if (acertou) acertos++ else {
                erros++
                errados += atual.entrada.titulo
            }
            AppContainer.escopo.launch { repositorio.registrarResposta(atual.entrada.id, acertou) }
        }
        _estado.value = atual.copy(
            feedback = when {
                acertou -> FeedbackRevisao.CORRETA
                naoLembrou -> FeedbackRevisao.NAO_LEMBRO
                else -> FeedbackRevisao.INCORRETA
            },
        )
    }

    fun avancar() {
        val atual = _estado.value as? RevisaoEstado.Cartao ?: return
        val feedback = atual.feedback ?: return
        val carta = cartas.removeFirstOrNull() ?: return
        if (feedback != FeedbackRevisao.CORRETA && recolocados.add(carta.id)) cartas.addLast(carta)

        if (cartas.isEmpty()) {
            viewModelScope.launch { _estado.value = resumo() }
        } else {
            _estado.value = proximoCartao()
        }
    }

    private fun proximoCartao() = RevisaoEstado.Cartao(
        entrada = cartas.first(),
        posicao = (respondidos.size + 1).coerceAtMost(total.coerceAtLeast(1)),
        total = total,
    )

    private suspend fun resumo(): RevisaoEstado.Resumo {
        val sequencia = repositorio.observarResumoDeRevisao().first()
        return RevisaoEstado.Resumo(acertos, erros, errados.toList(), sequencia.diasSeguidos, restantes)
    }

    private companion object { const val TETO_SESSAO = 20 }
}
