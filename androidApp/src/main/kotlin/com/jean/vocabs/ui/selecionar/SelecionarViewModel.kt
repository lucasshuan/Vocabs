package com.jean.vocabs.ui.selecionar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.Captura
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.duplicataDeAlvo
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
class SelecionarViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val preferencias = AppContainer.preferencias(app)
    private val procurado = MutableStateFlow(Procura())

    private data class Procura(val texto: String = "", val alvo: String = "")

    val duplicata: StateFlow<Entrada?> = combine(
        repositorio.observarProntas(Escopo.Todos),
        repositorio.observarInbox(Escopo.Todos),
        procurado,
    ) { prontas, inbox, busca ->
        if (busca.texto.isBlank()) return@combine null
        duplicataDeAlvo(busca.texto, (prontas + inbox).filter { it.par.alvo == busca.alvo })
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Os idiomas em que dá para guardar — a lista do seletor do cabeçalho. */
    val cursos: StateFlow<List<String>> = preferencias.observarCursos()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun observar(id: Long): StateFlow<Captura?> = repositorio.observarCapturaPorId(id)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun procurarDuplicata(texto: String, alvo: String) {
        procurado.value = Procura(texto, alvo)
    }

    /** Ainda dá para trocar aqui: nada nasceu neste par até o "Guardar". */
    fun trocarIdioma(id: Long, alvo: String) {
        AppContainer.escopo.launch { repositorio.alterarIdiomaDaCaptura(id, alvo) }
    }

    /**
     * Confirma e devolve os ids criados, que é o que a tela de confirmação
     * acompanha enquanto a IA trabalha. A geração segue no escopo do app: a
     * navegação acontece antes de a primeira ficha ficar pronta.
     */
    fun guardar(id: Long, trecho: String, alvos: List<AlvoSelecionado>, aoPronto: (List<Long>) -> Unit) {
        viewModelScope.launch {
            val ids = repositorio.confirmarCaptura(id, trecho, alvos)
            aoPronto(ids)
            AppContainer.escopo.launch { repositorio.gerarFichas(ids) }
        }
    }

    fun excluir(id: Long) {
        AppContainer.escopo.launch { repositorio.excluirCaptura(id) }
    }
}
