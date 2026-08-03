package com.jean.vocabs.ui.captura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.media.TranscritorDeAudio
import com.jean.vocabs.media.TranscritorDeFoto
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.ParIdiomas
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * O que a folha do `+` precisa saber: em que idiomas dá para guardar, e qual
 * deles vem marcado.
 *
 * O marcado é o curso aberto — que na Início é a página visível e nas outras
 * abas é o último usado. Não há um terceiro conceito de "último idioma de
 * captura": ele seria mais um estado para divergir do que a tela mostra.
 */
data class EstadoDaCaptura(
    val par: ParIdiomas = ParIdiomas.PADRAO,
    val cursos: List<String> = emptyList(),
)

class CapturaViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)
    private val preferencias = AppContainer.preferencias(app)
    private val transcritorFoto = TranscritorDeFoto(app)
    private val transcritorAudio = TranscritorDeAudio(app)

    val estado: StateFlow<EstadoDaCaptura> = combine(
        preferencias.observarPar(),
        preferencias.observarCursos(),
        ::EstadoDaCaptura,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EstadoDaCaptura())

    /**
     * Guarda o trecho e devolve a captura, que é para onde a seleção vai.
     *
     * O "Continuar" grava antes de haver seleção nenhuma de propósito: a partir
     * daqui, desistir da marcação, fechar o app ou perder a conexão deixa a
     * captura em Pendentes com o idioma já escolhido, em vez de jogar fora o que
     * a pessoa colou.
     */
    fun salvarTrecho(trecho: String, alvo: String, aoPronto: (Long) -> Unit) {
        val par = ParIdiomas(nativo = estado.value.par.nativo, alvo = alvo)
        viewModelScope.launch {
            aoPronto(repositorio.capturarTrecho(trecho, par))
        }
    }

    /**
     * OCR e voz são locais e demoram; a captura já está salva antes deles.
     *
     * Roda no escopo de aplicação porque a folha fecha na hora: preso ao
     * `viewModelScope`, o cancelamento da tela deixaria a captura para sempre em
     * TRANSCREVENDO.
     */
    fun salvarMidia(formato: FormatoCaptura, caminho: String, duracaoMs: Long?, alvo: String) {
        val par = ParIdiomas(nativo = estado.value.par.nativo, alvo = alvo)
        AppContainer.escopo.launch {
            val id = repositorio.capturarMidia(formato, caminho, duracaoMs, par)
            val resultado = when (formato) {
                FormatoCaptura.FOTO -> transcritorFoto.transcrever(caminho)
                FormatoCaptura.AUDIO -> transcritorAudio.transcrever(caminho)
                FormatoCaptura.TEXTO -> return@launch
            }
            repositorio.registrarTranscricao(id, resultado.texto, resultado.erro)
        }
    }
}
