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
    data object Loading : ReviewState
    data object Empty : ReviewState
    data class CardSurface(
        val entry: Entry,
        val answer: String = "",
        val feedback: ReviewFeedback? = null,
        val position: Int,
        val total: Int,
    ) : ReviewState
    data class Summary(
        val hits: Int,
        val misses: Int,
        val wrong: List<String>,
        val dayStreak: Int,
        val rest: Int,
    ) : ReviewState
}

class ReviewViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val _estado = MutableStateFlow<ReviewState>(ReviewState.Loading)
    val state: StateFlow<ReviewState> = _estado.asStateFlow()

    private var cards = ArrayDeque<Entry>()
    private val respondidos = mutableSetOf<Long>()
    private val restored = mutableSetOf<Long>()
    private var hits = 0
    private var misses = 0
    private var wrong = mutableListOf<String>()
    private var total = 0
    private var rest = 0

    init { newRound() }

    fun newRound() {
        viewModelScope.launch {
            _estado.value = ReviewState.Loading
            val queue = repository.observeReviewQueue().first()
            rest = (queue.size - SESSION_CAP).coerceAtLeast(0)
            hits = 0
            misses = 0
            wrong.clear()
            respondidos.clear()
            restored.clear()
            cards = ArrayDeque(queue.take(SESSION_CAP).shuffled())
            total = cards.size
            _estado.value = if (cards.isEmpty()) ReviewState.Empty else nextCard()
        }
    }

    fun editAnswer(value: String) {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (current.feedback == null) _estado.value = current.copy(answer = value)
    }

    fun confirm() {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (current.feedback != null || current.answer.isBlank()) return
        rate(isAnswerCorrect(current.answer, current.entry.target.orEmpty()), didntRemember = false)
    }

    fun dontRemember() {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (current.feedback == null) rate(correct = false, didntRemember = true)
    }

    private fun rate(correct: Boolean, didntRemember: Boolean) {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        if (respondidos.add(current.entry.id)) {
            if (correct) hits++ else {
                misses++
                wrong += current.entry.title
            }
            AppContainer.scope.launch { repository.recordAnswer(current.entry.id, correct) }
        }
        _estado.value = current.copy(
            feedback = when {
                correct -> ReviewFeedback.CORRETA
                didntRemember -> ReviewFeedback.NAO_LEMBRO
                else -> ReviewFeedback.INCORRETA
            },
        )
    }

    fun advance() {
        val current = _estado.value as? ReviewState.CardSurface ?: return
        val feedback = current.feedback ?: return
        val card = cards.removeFirstOrNull() ?: return
        if (feedback != ReviewFeedback.CORRETA && restored.add(card.id)) cards.addLast(card)

        if (cards.isEmpty()) {
            viewModelScope.launch { _estado.value = summary() }
        } else {
            _estado.value = nextCard()
        }
    }

    private fun nextCard() = ReviewState.CardSurface(
        entry = cards.first(),
        position = (respondidos.size + 1).coerceAtMost(total.coerceAtLeast(1)),
        total = total,
    )

    private suspend fun summary(): ReviewState.Summary {
        val streak = repository.observeReviewSummary().first()
        return ReviewState.Summary(hits, misses, wrong.toList(), streak.dayStreak, rest)
    }

    private companion object { const val SESSION_CAP = 20 }
}
