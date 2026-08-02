package com.jean.vocabs.ui.captura

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.jean.vocabs.shared.AppContainer
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
    private val alvoEmEdicao = MutableStateFlow("")

    val duplicata: StateFlow<Entrada?> =
        combine(
            repositorio.observarProntas(),
            repositorio.observarInbox(),
            alvoEmEdicao,
        ) { prontas, inbox, alvo ->
            duplicataDeAlvo(alvo = alvo, entradas = prontas + inbox)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun procurarDuplicata(alvo: String) {
        alvoEmEdicao.value = alvo
    }

    /**
     * Grava e dispara a geração no escopo da aplicação, não no viewModelScope.
     *
     * A tela é fechada imediatamente após salvar (princípio 2 do documento:
     * captura e revisão são momentos separados). Se o trabalho estivesse preso
     * ao ciclo de vida desta tela, fechá-la cancelaria a geração e a entrada
     * ficaria travada em PENDENTE.
     */
    fun salvarTexto(trecho: String, alvo: String, origem: String) {
        AppContainer.escopo.launch {
            val id = repositorio.capturarTexto(
                trecho = trecho,
                alvo = alvo,
                origem = origem.ifBlank { null },
            )
            repositorio.gerarFicha(id)
        }
    }

    /**
     * Foto e áudio não chamam a IA: entram como rascunho e esperam você
     * transcrever. É o que permite capturar em segundos sem sair do que estava
     * fazendo — o critério da Fase 1.5.
     */
    fun salvarMidia(formato: FormatoCaptura, caminho: String, origem: String) {
        AppContainer.escopo.launch {
            repositorio.capturarMidia(
                formato = formato,
                caminho = caminho,
                origem = origem.ifBlank { null },
            )
        }
    }
}
