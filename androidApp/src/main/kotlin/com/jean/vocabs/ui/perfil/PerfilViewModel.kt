package com.jean.vocabs.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.Escopo
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.ResumoCurso
import com.jean.vocabs.shared.domain.UsoIa
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * A tela Você: o total primeiro, a quebra por idioma depois.
 *
 * Sequência e estoque são hábito, e hábito é da pessoa e não do curso — quem
 * estudou espanhol ontem e francês hoje estudou dois dias seguidos. Por isso os
 * três números do topo somam tudo, e a lista logo abaixo é que reparte.
 */
data class PerfilEstado(
    val par: ParIdiomas = ParIdiomas.PADRAO,
    /** Todos os cursos matriculados, na ordem da faixa — inclusive os vazios. */
    val cursos: List<ResumoCurso> = emptyList(),
    val diasSeguidos: Int = 0,
    val usoIa: UsoIa = UsoIa("", 0),
) {
    val totalDeFichas: Int get() = cursos.sumOf { it.total }
    val totalDeDominadas: Int get() = cursos.sumOf { it.dominadas }
}

class PerfilViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val preferencias = AppContainer.preferencias(app)

    /**
     * A faixa vem da preferência, e não do que existe no banco.
     *
     * Um curso recém-criado não tem nenhuma palavra. Montar a lista a partir das
     * fichas faria o idioma que a pessoa acabou de escolher desaparecer no
     * instante seguinte — e ela ficaria sem por onde voltar.
     */
    private val cursos = combine(
        preferencias.observarCursos(),
        preferencias.observarPar(),
        repositorio.observarCursos(),
    ) { matriculados, par, comFichas ->
        matriculados.map { alvo ->
            val curso = ParIdiomas(nativo = par.nativo, alvo = alvo)
            comFichas.firstOrNull { it.par == curso } ?: ResumoCurso(curso, total = 0, dominadas = 0)
        }
    }

    val estado: StateFlow<PerfilEstado> = combine(
        cursos,
        preferencias.observarPar(),
        // De todos os cursos: a sequência de dias conta atividade em qualquer
        // idioma, e é o único número desta tela que já era assim antes.
        repositorio.observarResumoDeRevisao(Escopo.Todos),
        repositorio.observarUsoIa(),
    ) { listaDeCursos, par, revisao, usoIa ->
        PerfilEstado(
            par = par,
            cursos = listaDeCursos,
            diasSeguidos = revisao.diasSeguidos,
            usoIa = usoIa,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilEstado())

    /** Quantos idiomas ainda dá para escolher — o "37 idiomas" da tela Novo idioma. */
    val disponiveis: StateFlow<Int> = preferencias.observarCursos()
        .map { matriculados -> com.jean.vocabs.contracts.Idiomas.CATALOGO.count { it.codigo !in matriculados } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
