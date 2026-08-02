package com.jean.vocabs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.jean.vocabs.shared.AppContainer
import com.jean.vocabs.ui.VocabsApp
import com.jean.vocabs.ui.theme.VocabsTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Antes de qualquer tela: o repositório é criado na primeira vez que uma
        // ViewModel o pede, e nesse momento a URL do servidor já precisa estar
        // decidida. O endereço em si o AppContainer escolhe sozinho, conforme o
        // app esteja num emulador ou num aparelho de verdade.
        AppContainer.configurar(
            servidorLan = BuildConfig.SERVIDOR_LAN,
            token = BuildConfig.APP_TOKEN,
        )

        // O conteúdo desenha atrás das barras do sistema; cada tela aplica os
        // insets que precisa (statusBarsPadding, navigationBarsPadding, imePadding).
        enableEdgeToEdge()
        setContent {
            VocabsTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    VocabsApp()
                }
            }
        }
    }
}
