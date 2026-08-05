package com.jean.vocabs.shared

import android.content.Context
import com.jean.vocabs.shared.data.VocabRepositoryImpl
import com.jean.vocabs.shared.data.local.AndroidDatabaseDriverFactory
import com.jean.vocabs.shared.data.remote.CardApi
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.VocabRepository
import com.jean.vocabs.shared.media.MediaFiles
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json

/**
 * Manual composition root. Koin or Hilt for four objects would be ceremony
 * without return; it can be swapped in later without touching the rest.
 */
object AppContainer {

    /**
     * The server as a physical device sees it: `machine-ip:port`.
     *
     * Comes from the build (`BuildConfig.LAN_SERVER`), which detects the
     * machine's IP on the local network or reads `SERVER_LAN` from `.env`. Empty
     * when there was no network at compile time.
     */
    var lanServer: String = ""

    /** Must match the APP_TOKEN set in the server's environment. */
    var token: String = "local-test-token"

    /**
     * Overrides URL and token when the cloud arrives — today only `androidApp`
     * calls this, passing what the build baked into the APK.
     */
    fun configure(lanServer: String, token: String) {
        this.lanServer = lanServer
        this.token = token
    }

    /**
     * 10.0.2.2 is how the emulator sees your machine's localhost — an address that
     * exists on no real device. Choosing wrong gives no clear error: it gives a
     * timeout, which is easy to mistake for a server that is down.
     */
    private val baseUrl: String
        get() = if (Device.isEmulator) {
            "http://10.0.2.2:8080"
        } else {
            "http://${lanServer.ifBlank { "10.0.2.2:8080" }}"
        }

    /**
     * Application scope for card generation.
     *
     * Cannot be the capture screen's viewModelScope: it closes immediately after
     * saving by design, and the cancelled scope would leave the entry stuck in
     * PENDING forever.
     */
    val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var repository: VocabRepository? = null

    @Volatile
    private var preferences: Preferences? = null

    fun repository(context: Context): VocabRepository =
        repository ?: synchronized(this) {
            repository ?: create(context.applicationContext).also { repository = it }
        }

    fun preferences(context: Context): Preferences =
        preferences ?: synchronized(this) {
            preferences ?: Preferences(context.applicationContext).also { preferences = it }
        }

    private fun create(context: Context): VocabRepository {
        val driver = AndroidDatabaseDriverFactory(context).create()
        val http = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                // AI generation is slow. Ktor's default would drop the request
                // midway and the card would never arrive.
                requestTimeoutMillis = 90_000
                connectTimeoutMillis = 15_000
                socketTimeoutMillis = 90_000
            }
        }

        return VocabRepositoryImpl(
            db = VocabsDatabase(driver),
            api = CardApi(baseUrl = baseUrl, token = token, client = http),
            io = Dispatchers.IO,
            now = { System.currentTimeMillis() },
            activeCourse = preferences(context).observeLanguagePair(),
            removeFile = MediaFiles::remove,
        )
    }
}
