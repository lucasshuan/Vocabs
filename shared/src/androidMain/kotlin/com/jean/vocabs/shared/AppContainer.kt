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
 * Composition root manual. Koin ou Hilt para quatro objetos seria cerimônia sem
 * retorno — quando a Fase 3 trouxer mais peças, dá para trocar sem tocar no resto.
 */
object AppContainer {

    /**
     * O servidor visto de um aparelho físico: `IP-da-máquina:porta`.
     *
     * Vem do build (`BuildConfig.SERVIDOR_LAN`), que detecta o IP da máquina na
     * rede local ou lê `SERVIDOR_LAN` do `.env`. Fica vazio quando não há rede
     * nenhuma na hora de compilar.
     */
    var lanServer: String = ""

    /** Precisa bater com o APP_TOKEN definido no ambiente do servidor. */
    var token: String = "token-de-teste-local"

    /**
     * Sobrescreve URL e token quando a nuvem entrar — hoje só o `androidApp`
     * chama isto, passando o que o build assou no APK.
     */
    fun configurar(lanServer: String, token: String) {
        this.lanServer = lanServer
        this.token = token
    }

    /**
     * 10.0.2.2 é como o emulador enxerga o localhost da sua máquina — um endereço
     * que não existe em aparelho nenhum. Escolher errado não dá erro claro: dá
     * timeout, que é fácil confundir com servidor fora do ar.
     */
    private val baseUrl: String
        get() = if (Device.isEmulator) {
            "http://10.0.2.2:8080"
        } else {
            "http://${lanServer.ifBlank { "10.0.2.2:8080" }}"
        }

    /**
     * Scope de aplicação para a geração da ficha.
     *
     * Não pode ser o viewModelScope da tela de captura: assim que ela é fechada
     * (o que acontece imediatamente após salvar, por design), o escopo seria
     * cancelado e a entrada ficaria presa em PENDING para sempre.
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
                // Geração com IA é lenta. O default do Ktor derrubaria a
                // requisição no meio e a ficha nunca chegaria.
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
