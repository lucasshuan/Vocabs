package io.github.lucasshuan.vocabu.shared

import android.content.Context
import io.github.lucasshuan.vocabu.shared.data.VocabRepositoryImpl
import io.github.lucasshuan.vocabu.shared.data.local.AndroidDatabaseDriverFactory
import io.github.lucasshuan.vocabu.shared.data.remote.CardApi
import io.github.lucasshuan.vocabu.shared.db.VocabsDatabase
import io.github.lucasshuan.vocabu.shared.domain.VocabRepository
import io.github.lucasshuan.vocabu.shared.media.MediaFiles
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
     * `machine-ip:port`, from `BuildConfig.LAN_SERVER`. Empty when there was no
     * network at compile time.
     */
    var lanServer: String = ""

    /** Must match the server's APP_TOKEN. */
    var token: String = "local-test-token"

    fun configure(lanServer: String, token: String) {
        this.lanServer = lanServer
        this.token = token
    }

    /**
     * 10.0.2.2 is the emulator's view of the host's localhost, and exists on no
     * real device. The wrong choice gives a timeout, not an error.
     */
    private val baseUrl: String
        get() = if (Device.isEmulator) {
            "http://10.0.2.2:8080"
        } else {
            "http://${lanServer.ifBlank { "10.0.2.2:8080" }}"
        }

    /**
     * Generation cannot run on the capture screen's viewModelScope: that screen
     * closes on save, and the cancelled scope leaves the entry stuck in PENDING.
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
                // Generation outlasts Ktor's default, which drops the request
                // mid-flight and the card never arrives.
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
