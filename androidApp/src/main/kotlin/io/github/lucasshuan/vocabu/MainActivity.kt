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

    // No `android:configChanges` for locale on this activity, deliberately: a
    // language change has to recreate so every `stringResource` re-reads.
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(UiLanguage.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Before any screen: the repository is created the first time a ViewModel
        // asks for it, and by then the server URL has to be decided.
        AppContainer.configure(
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
