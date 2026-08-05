package com.jean.vocabs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.ui.VocabsApp
import com.jean.vocabs.ui.theme.VocabsTheme
import com.jean.vocabs.ui.theme.darkAccordingTo

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Antes de qualquer tela: o repositório é criado na primeira vez que uma
        // ViewModel o pede, e nesse momento a URL do servidor já precisa estar
        // decidida. O endereço em si o AppContainer escolhe sozinho, conforme o
        // app esteja num emulador ou num aparelho de verdade.
        AppContainer.configurar(
            lanServer = BuildConfig.LAN_SERVER,
            token = BuildConfig.APP_TOKEN,
        )

        // O conteúdo desenha atrás das barras do sistema; cada tela aplica os
        // insets que precisa (statusBarsPadding, navigationBarsPadding, imePadding).
        enableEdgeToEdge()
        val preferences = AppContainer.preferences(this)
        setContent {
            // O valor inicial vem da leitura síncrona, e não de um `null` que o
            // fluxo preenche depois: um default enquanto o disco responde faria
            // o app abrir no claro e piscar para o escuro no primeiro frame.
            val theme by preferences.observeTheme()
                .collectAsStateWithLifecycle(initialValue = preferences.theme)

            VocabsTheme(temaEscuro = darkAccordingTo(theme)) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    VocabsApp()
                }
            }
        }
    }
}
