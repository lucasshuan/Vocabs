package com.jean.vocabs.ui.captura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.media.TranscritorDeAudio
import com.jean.vocabs.media.TranscritorDeFoto
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.shared.domain.AlvoSelecionado
import com.jean.vocabs.shared.domain.Entrada
import com.jean.vocabs.shared.domain.FormatoCaptura
import com.jean.vocabs.shared.domain.duplicataDeAlvo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CapturaViewModel(app: Application) : AndroidViewModel(app) {

    private val repositorio = AppContainer.repositorio(app)
    private val transcritorFoto = TranscritorDeFoto(app)
    private val transcritorAudio = TranscritorDeAudio(app)
    private val alvoEmEdicao = MutableStateFlow("")

    val duplicata: StateFlow<Entrada?> = combine(
        repositorio.observarProntas(),
        repositorio.observarInbox(),
        alvoEmEdicao,
    ) { prontas, inbox, alvo ->
        duplicataDeAlvo(alvo = alvo, entradas = prontas + inbox)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun procurarDuplicata(alvo: String) {
        alvoEmEdicao.value = alvo
    }

    /** A transação termina antes de a tela fechar; as gerações continuam no app. */
    fun salvarTexto(trecho: String, alvos: List<AlvoSelecionado>) {
        AppContainer.escopo.launch {
            val ids = repositorio.capturarTexto(trecho = trecho, alvos = alvos)
            repositorio.gerarFichas(ids)
        }
    }

    /** OCR e voz são locais; qualquer falha deixa a captura editável em Pendentes. */
    fun salvarMidia(formato: FormatoCaptura, caminho: String, duracaoMs: Long?) {
        AppContainer.escopo.launch {
            val id = repositorio.capturarMidia(formato, caminho, duracaoMs)
            val resultado = when (formato) {
                FormatoCaptura.FOTO -> transcritorFoto.transcrever(caminho)
                FormatoCaptura.AUDIO -> transcritorAudio.transcrever(caminho)
                FormatoCaptura.TEXTO -> return@launch
            }
            repositorio.registrarTranscricao(id, resultado.texto, resultado.erro)
        }
    }

}
