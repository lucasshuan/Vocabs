package com.jean.vocabs.shared

import android.content.Context
import com.jean.vocabs.shared.data.VocabRepositoryImpl
import com.jean.vocabs.shared.data.local.AndroidDatabaseDriverFactory
import com.jean.vocabs.shared.data.remote.FichaApi
import com.jean.vocabs.shared.db.VocabsDatabase
import com.jean.vocabs.shared.domain.VocabRepository
import com.jean.vocabs.shared.media.ArquivosDeMidia
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
    var servidorLan: String = ""

    /** Precisa bater com o APP_TOKEN definido no ambiente do servidor. */
    var token: String = "token-de-teste-local"

    /**
     * Sobrescreve URL e token quando a nuvem entrar — hoje só o `androidApp`
     * chama isto, passando o que o build assou no APK.
     */
    fun configurar(servidorLan: String, token: String) {
        this.servidorLan = servidorLan
        this.token = token
    }

    /**
     * 10.0.2.2 é como o emulador enxerga o localhost da sua máquina — um endereço
     * que não existe em aparelho nenhum. Escolher errado não dá erro claro: dá
     * timeout, que é fácil confundir com servidor fora do ar.
     */
    private val baseUrl: String
        get() = if (Ambiente.ehEmulador) {
            "http://10.0.2.2:8080"
        } else {
            "http://${servidorLan.ifBlank { "10.0.2.2:8080" }}"
        }

    /**
     * Escopo de aplicação para a geração da ficha.
     *
     * Não pode ser o viewModelScope da tela de captura: assim que ela é fechada
     * (o que acontece imediatamente após salvar, por design), o escopo seria
     * cancelado e a entrada ficaria presa em PENDING para sempre.
     */
    val escopo: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile
    private var repositorio: VocabRepository? = null

    @Volatile
    private var preferencias: Preferencias? = null

    fun repositorio(context: Context): VocabRepository =
        repositorio ?: synchronized(this) {
            repositorio ?: criar(context.applicationContext).also { repositorio = it }
        }

    fun preferencias(context: Context): Preferencias =
        preferencias ?: synchronized(this) {
            preferencias ?: Preferencias(context.applicationContext).also { preferencias = it }
        }

    private fun criar(context: Context): VocabRepository {
        val driver = AndroidDatabaseDriverFactory(context).criar()
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
            api = FichaApi(baseUrl = baseUrl, token = token, client = http),
            io = Dispatchers.IO,
            agora = { System.currentTimeMillis() },
            cursoAtivo = preferencias(context).observarPar(),
            removerArquivo = ArquivosDeMidia::remover,
        )
    }
}
