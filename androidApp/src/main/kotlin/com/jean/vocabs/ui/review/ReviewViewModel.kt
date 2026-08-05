package com.jean.vocabs.ui.review

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.isAnswerCorrect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class ReviewFeedback { CORRETA, INCORRETA, NAO_LEMBRO }

sealed interface ReviewState {
    data object Carregando : ReviewState
    data object Empty : ReviewState
    data class CardSurface(
        val entry: Entry,
        val answer: String = "",
        val feedback: ReviewFeedback? = null,
        val posicao: Int,
        val total: Int,
    ) : ReviewState
    data class Summary(
        val hits: Int,
        val errors: Int,
        val errados: List<String>,
        val dayStreak: Int,
        val restantes: Int,
    ) : ReviewState
}

class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val _estado = MutableStateFlow<ReviewState>(ReviewState.Carregando)
    val estado: StateFlow<ReviewState> = _estado.asStateFlow()

    private var cartas = ArrayDeque<Entry>()
    private val respondidos = mutableSetOf<Long>()
    private val recolocados = mutableSetOf<Long>()
    private var hits = 0
    private var errors = 0
    private var errados = mutableListOf<String>()
    private var total = 0
    private var restantes = 0

    init { novaRodada() }

    fun novaRodada() {
        viewModelScope.launch {
            _estado.value = ReviewState.Carregando
            val fila = repository.observeReviewQueue().first()
            restantes = (fila.size - TETO_SESSAO).coerceAtLeast(0)
            hits = 0
            errors = 0
            errados.clear()
            respondidos.clear()
            recolocados.clear()
            cartas = ArrayDeque(fila.take(TETO_SESSAO).shuffled())
            total = cartas.size
            _estado.value = if (cartas.isEmpty()) ReviewState.Empty else proximoCartao()
        }
    }

    fun editarResposta(value: String) {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (current.feedback == null) _estado.value = current.copy(answer = value)
    }

    fun confirmar() {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (current.feedback != null || current.answer.isBlank()) return
        avaliar(isAnswerCorrect(current.answer, current.entry.target.orEmpty()), naoLembrou = false)
    }

    fun naoLembro() {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (current.feedback == null) avaliar(acertou = false, naoLembrou = true)
    }

    private fun avaliar(acertou: Boolean, naoLembrou: Boolean) {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (respondidos.add(current.entry.id)) {
            if (acertou) hits++ else {
                errors++
                errados += current.entry.title
            }
            AppContainer.scope.launch { repository.recordAnswer(current.entry.id, acertou) }
        }
        _estado.value = current.copy(
            feedback = when {
                acertou -> ReviewFeedback.CORRETA
                naoLembrou -> ReviewFeedback.NAO_LEMBRO
                else -> ReviewFeedback.INCORRETA
            },
        )
    }

    fun avancar() {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        val feedback = current.feedback ?: return
        val carta = cartas.removeFirstOrNull() ?: return
        if (feedback != ReviewFeedback.CORRETA && recolocados.add(carta.id)) cartas.addLast(carta)

        if (cartas.isEmpty()) {
            viewModelScope.launch { _estado.value = resumo() }
        } else {
            _estado.value = proximoCartao()
        }
    }

    private fun proximoCartao() = ReviewState.CardSurface(
        entry = cartas.first(),
        posicao = (respondidos.size + 1).coerceAtMost(total.coerceAtLeast(1)),
        total = total,
    )

    private suspend fun resumo(): ReviewState.Summary {
        val streak = repository.observeReviewSummary().first()
        return ReviewState.Summary(hits, errors, errados.toList(), streak.dayStreak, restantes)
    }

    private companion object { const val TETO_SESSAO = 20 }
}
