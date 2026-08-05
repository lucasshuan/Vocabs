package com.jean.vocabs.ui.guardado

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.EntryStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

/**
 * O que acabou de ser guardado, enquanto a IA trabalha.
 *
 * [totalDoCurso] é o estoque **depois** da captura — "26 fichas agora". O
 * número existe para fechar o ciclo: a pessoa acabou de acrescentar duas coisas
 * a alguma coisa, e o tamanho dessa coisa é o que dá sentido ao gesto.
 */
data class GuardadoEstado(
    val entradas: List<Entrada> = emptyList(),
    val totalDoCurso: Int = 0,
) {
    val alvo: String get() = entradas.firstOrNull()?.par?.alvo.orEmpty()

    /** Enquanto houver ficha em construção há o que olhar, e a tela não se fecha sozinha. */
    val trabalhando: Boolean
        get() = entradas.any { it.status == EntryStatus.PENDING || it.status == EntryStatus.GENERATING }
}

@OptIn(ExperimentalCoroutinesApi::class)
class GuardadoViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val ids = MutableStateFlow<List<Long>>(emptyList())

    val estado: StateFlow<GuardadoEstado> = ids.flatMapLatest { lista ->
        combine(
            repositorio.observarEntradas(lista),
            repositorio.observarProntas(Escopo.Todos),
        ) { entradas, prontas ->
            val curso = entradas.firstOrNull()?.par
            GuardadoEstado(
                entradas = entradas,
                totalDoCurso = prontas.count { it.par == curso },
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), GuardadoEstado())

    fun acompanhar(lista: List<Long>) {
        if (ids.value != lista) ids.value = lista
    }
}
