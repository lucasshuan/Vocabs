package com.jean.vocabs.ui.pending

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.ui.components.captureTitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Pendentes é uma fila, não uma notificação por curso.
 *
 * Ela nunca é recortada por idioma — nem pelo curso aberto, nem por um filtro
 * que tenha ficado ligado da visita passada. O que chega aqui é trabalho parado,
 * e trabalho parado num idioma não deixa de existir porque a pessoa foi estudar
 * outro. O selo da aba conta tudo pela mesma razão.
 */
data class PendingState(
    val captures: List<Capture> = emptyList(),
    val cards: List<Entry> = emptyList(),
) {
    val total: Int get() = captures.size + cards.size

    /** Quantas capturas cruas por idioma — o número que cada chip de filtro mostra. */
    val byLanguage: Map<String, Int>
        get() = (captures.map { it.languagePair.target } + cards.map { it.languagePair.target })
            .groupingBy { it }
            .eachCount()
}

/**
 * Uma exclusão que já saiu da tela e ainda pode voltar.
 *
 * [chave] existe pelo mesmo motivo que a do aviso de captura: duas exclusões
 * seguidas do mesmo tipo precisam contar como eventos diferentes para a faixa
 * reiniciar a contagem em vez de continuar a anterior.
 */
data class PendingDeletion(
    val key: Long,
    val id: Long,
    val isCapture: Boolean,
    val title: String,
)

/**
 * Quanto tempo o "Desfazer" fica de pé.
 *
 * Os mesmos 5 s do aviso de captura, e não é coincidência: é o intervalo que o
 * app já ensinou como "o tempo que uma faixa dura". A barra que corre no rodapé
 * da faixa mostra quanto sobra, para que ignorá-la seja uma escolha e não um
 * susto.
 */
private const val UNDO_WINDOW_MS = 5_000L

class PendingViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)

    private val _exclusao = MutableStateFlow<PendingDeletion?>(null)

    /** A última exclusão ainda dentro da janela de arrependimento, se houver. */
    val deletion: StateFlow<PendingDeletion?> = _exclusao.asStateFlow()

    private var count: Job? = null

    /**
     * A fila **já sem** o que acabou de ser arrastado para fora.
     *
     * O item some da lista no instante do gesto, muito antes de o banco saber
     * disso: quem arrastou precisa ver a fila diminuir na hora, e a exclusão de
     * verdade só acontece quando a janela de desfazer fecha. Como o selo da aba
     * sai de `total`, que sai daqui, os dois nunca discordam.
     */
    val state: StateFlow<PendingState> = combine(
        repository.observePendingCaptures(Scope.All),
        repository.observeInbox(Scope.All),
        _exclusao,
    ) { captures, cards, deletion ->
        PendingState(
            captures = captures.filterNot { deletion != null && deletion.isCapture && deletion.id == it.id },
            cards = cards.filterNot { deletion != null && !deletion.isCapture && deletion.id == it.id },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PendingState())

    val total: StateFlow<Int> = state.map { it.total }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun tryAgain(id: Long) {
        AppContainer.scope.launch { repository.generateCard(id) }
    }

    fun deleteCapture(capture: Capture) {
        schedule(PendingDeletion(System.nanoTime(), capture.id, isCapture = true, title = captureTitle(capture)))
    }

    fun deleteCard(entry: Entry) {
        schedule(PendingDeletion(System.nanoTime(), entry.id, isCapture = false, title = entry.title))
    }

    /** O gesto foi um engano: o item volta para a fila e nada chega ao banco. */
    fun undo() {
        count?.cancel()
        count = null
        _exclusao.value = null
    }

    /**
     * Uma exclusão de cada vez.
     *
     * Quem arrasta o segundo cartão antes de a faixa do primeiro sumir **confirma**
     * o primeiro — empilhar faixas obrigaria a ler três confirmações para limpar
     * três linhas, e é justamente limpar a fila em série que o gesto veio permitir.
     */
    private fun schedule(fresh: PendingDeletion) {
        count?.cancel()
        _exclusao.value?.let(::confirm)
        _exclusao.value = fresh
        count = viewModelScope.launch {
            delay(UNDO_WINDOW_MS)
            confirm(fresh)
            _exclusao.compareAndSet(fresh, null)
        }
    }

    /**
     * A janela fechou — daqui não volta: a captura leva junto o arquivo de mídia.
     *
     * O apagamento em si corre no escopo do app, e não no da tela, porque fechar
     * Pendentes no meio da chamada deixaria a linha apagada pela metade. Já a
     * contagem dos 5 s é do ViewModel de propósito: se o processo morrer antes de
     * ela terminar, o item simplesmente continua na fila — o erro seguro dos dois.
     */
    private fun confirm(deletion: PendingDeletion) {
        AppContainer.scope.launch {
            if (deletion.isCapture) repository.deleteCapture(deletion.id) else repository.delete(deletion.id)
        }
    }
}
