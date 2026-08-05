package com.jean.vocabs.ui.select

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Capture
import com.jean.vocabs.shared.domain.Entry
import com.jean.vocabs.shared.domain.Scope
import com.jean.vocabs.shared.domain.SelectedTarget
import com.jean.vocabs.shared.domain.duplicateOfTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A tela de seleção: marcar o que chamou atenção numa captura já guardada.
 *
 * A duplicata é procurada **no idioma de destino**, e não na coleção inteira:
 * "carne" existir em espanhol não é motivo para avisar quem está guardando
 * "carne" em italiano. Por isso o alvo procurado carrega o curso junto.
 */
class SelectViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)
    private val wanted = MutableStateFlow(Procura())

    private data class Procura(val text: String = "", val target: String = "")

    val duplicata: StateFlow<Entry?> = combine(
        repository.observeReady(Scope.All),
        repository.observeInbox(Scope.All),
        wanted,
    ) { prontas, inbox, busca ->
        if (busca.text.isBlank()) return@combine null
        duplicateOfTarget(busca.text, (prontas + inbox).filter { it.languagePair.target == busca.target })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Os idiomas em que dá para guardar — a lista do seletor do cabeçalho. */
    val courses: StateFlow<List<String>> = preferences.observeCourses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observar(id: Long): StateFlow<Capture?> = repository.observeCaptureById(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun procurarDuplicata(text: String, target: String) {
        wanted.value = Procura(text, target)
    }

    /** Ainda dá para trocar aqui: nada nasceu neste par até o "Guardar". */
    fun trocarIdioma(id: Long, target: String) {
        AppContainer.scope.launch { repository.changeCaptureLanguage(id, target) }
    }

    /**
     * Confirma e devolve os ids criados, que é o que a tela de confirmação
     * acompanha enquanto a IA trabalha. A geração segue no escopo do app: a
     * navegação acontece antes de a primeira ficha ficar pronta.
     */
    fun guardar(id: Long, snippet: String, alvos: List<SelectedTarget>, aoPronto: (List<Long>) -> Unit) {
        viewModelScope.launch {
            val ids = repository.confirmCapture(id, snippet, alvos)
            aoPronto(ids)
            AppContainer.scope.launch { repository.generateCards(ids) }
        }
    }

    fun excluir(id: Long) {
        AppContainer.scope.launch { repository.deleteCapture(id) }
    }
}
