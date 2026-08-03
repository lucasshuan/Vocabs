package com.jean.vocabs.ui.perfil

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.ParIdiomas
import com.jean.vocabs.shared.domain.QuotaDoDia
import com.jean.vocabs.shared.domain.ResumoCurso
import com.jean.vocabs.shared.domain.UsoIa
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * A tela Você: com que idiomas a pessoa está, e um resumo do curso aberto.
 *
 * Os números daqui são os três do handoff, e nenhum deles é a mesma coisa que os
 * da tela de Progresso: aqui é a vitrine, lá é a página inteira.
 */
data class PerfilEstado(
    val par: ParIdiomas = ParIdiomas.PADRAO,
    /** Todos os cursos matriculados, na ordem da faixa — inclusive os vazios. */
    val cursos: List<ResumoCurso> = emptyList(),
    val diasSeguidos: Int = 0,
    val quota: QuotaDoDia = QuotaDoDia(feita = 0, naFila = 0),
    val usoIa: UsoIa = UsoIa("", 0),
    val exportando: Boolean = false,
) {
    val cursoAtual: ResumoCurso?
        get() = cursos.firstOrNull { it.par == par }
}

class PerfilViewModel(app: Application) : AndroidViewModel(app) {
    private val repositorio = AppContainer.repositorio(app)
    private val preferencias = AppContainer.preferencias(app)
    private val exportando = MutableStateFlow(false)

    /**
     * A faixa vem da preferência, e não do que existe no banco.
     *
     * Um curso recém-criado não tem nenhuma palavra. Montar a faixa a partir das
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
        repositorio.observarResumoDeRevisao(),
        repositorio.observarUsoIa(),
        exportando,
    ) { listaDeCursos, par, revisao, usoIa, estaExportando ->
        PerfilEstado(
            par = par,
            cursos = listaDeCursos,
            diasSeguidos = revisao.diasSeguidos,
            quota = revisao.quota,
            usoIa = usoIa,
            exportando = estaExportando,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PerfilEstado())

    /** Quantos idiomas ainda dá para escolher — o "37 idiomas" da tela Novo idioma. */
    val disponiveis: StateFlow<Int> = preferencias.observarCursos()
        .map { matriculados -> com.jean.vocabs.contracts.Idiomas.CATALOGO.count { it.codigo !in matriculados } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun abrirCurso(codigo: String) = preferencias.abrirCurso(codigo)

    fun exportar(aoPronto: (File) -> Unit, aoErro: (String) -> Unit) {
        if (exportando.value) return
        viewModelScope.launch {
            exportando.value = true
            runCatching {
                ExportadorTagarara.criar(getApplication(), repositorio.dadosParaExportacao())
            }.onSuccess(aoPronto).onFailure { aoErro(it.message ?: "Não foi possível exportar os dados.") }
            exportando.value = false
        }
    }
}
