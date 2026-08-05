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

        // Before any screen: the repository is created the first time a ViewModel
        // asks for it, and by then the server URL has to be decided.
        AppContainer.configurar(
            lanServer = BuildConfig.LAN_SERVER,
            token = BuildConfig.APP_TOKEN,
        )

        // Content draws behind the system bars; each screen applies the insets it
        // needs (statusBarsPadding, navigationBarsPadding, imePadding).
        enableEdgeToEdge()
        val preferences = AppContainer.preferences(this)
        setContent {
            // The initial value comes from the synchronous read rather than a
            // `null` the flow fills in later: a default while the disk answers
            // would open the app in light and flash to dark on the first frame.
            val theme by preferences.observeTheme()
                .collectAsStateWithLifecycle(initialValue = preferences.theme)

            VocabsTheme(darkTheme = darkAccordingTo(theme)) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    VocabsApp()
                }
            }
        }
    }
}
