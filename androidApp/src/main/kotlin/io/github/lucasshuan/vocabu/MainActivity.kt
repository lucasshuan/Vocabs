package io.github.lucasshuan.vocabu

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.lucasshuan.vocabu.shared.AppContainer
import io.github.lucasshuan.vocabu.ui.VocabsApp
import io.github.lucasshuan.vocabu.ui.language.UiLanguage
import io.github.lucasshuan.vocabu.ui.theme.VocabsTheme
import io.github.lucasshuan.vocabu.ui.theme.darkAccordingTo

class MainActivity : ComponentActivity() {

    // No `android:configChanges` for locale here: a language change has to
    // recreate so every `stringResource` re-reads.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(UiLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Before any screen: the repository is built on the first ViewModel that
        // asks, and by then the server URL has to be decided.
        AppContainer.configure(
            lanServer = BuildConfig.LAN_SERVER,
            token = BuildConfig.APP_TOKEN,
        )

        // Content draws behind the system bars; each screen applies its own insets.
        enableEdgeToEdge()
        val preferences = AppContainer.preferences(this)
        setContent {
            // Seeded from the synchronous read: a default while the disk answers
            // opens the app in light and flashes to dark on the first frame.
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
