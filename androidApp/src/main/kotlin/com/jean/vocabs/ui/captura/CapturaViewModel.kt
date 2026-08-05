package com.jean.vocabs.ui.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.media.TranscritorDeAudio
import com.jean.vocabs.media.TranscritorDeFoto
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.CaptureFormat
import com.jean.vocabs.shared.domain.LanguagePair
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
    val languagePair: LanguagePair = LanguagePair.PADRAO,
    val courses: List<String> = emptyList(),
)

class CapturaViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = AppContainer.repository(app)
    private val preferences = AppContainer.preferences(app)
    private val transcritorFoto = TranscritorDeFoto(app)
    private val transcritorAudio = TranscritorDeAudio(app)

    val estado: StateFlow<EstadoDaCaptura> = combine(
        preferences.observeLanguagePair(),
        preferences.observeCourses(),
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
    fun salvarTrecho(snippet: String, target: String, aoPronto: (Long) -> Unit) {
        val languagePair = LanguagePair(native = estado.value.languagePair.native, target = target)
        viewModelScope.launch {
            aoPronto(repository.captureSnippet(snippet, languagePair))
        }
    }

    /**
     * OCR e voz são locais e demoram; a captura já está salva antes deles.
     *
     * Roda no escopo de aplicação porque a folha fecha na hora: preso ao
     * `viewModelScope`, o cancelamento da tela deixaria a captura para sempre em
     * TRANSCRIBING.
     */
    fun salvarMidia(
        format: CaptureFormat,
        path: String,
        durationMs: Long?,
        target: String,
        aoIdentificar: (Long) -> Unit = {},
    ) {
        val languagePair = LanguagePair(native = estado.value.languagePair.native, target = target)
        AppContainer.scope.launch {
            val id = repository.captureMedia(format, path, durationMs, languagePair)
            aoIdentificar(id)
            val result = when (format) {
                CaptureFormat.PHOTO -> transcritorFoto.transcrever(path)
                CaptureFormat.AUDIO -> transcritorAudio.transcrever(path)
                CaptureFormat.TEXT -> return@launch
            }
            repository.recordTranscription(id, result.text, result.error)
        }
    }
}
