import java.net.Inet4Address
import java.net.NetworkInterface
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 tem Kotlin embutido: aplicar 'org.jetbrains.kotlin.android' aqui é erro.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Lê uma chave do `.env` da raiz, com a variável de ambiente tendo precedência.
 *
 * Mesma semântica do `Config` do servidor, mas resolvida em tempo de compilação:
 * o celular não tem como ler o `.env` da sua máquina, então o valor precisa ser
 * assado no APK.
 */
fun doEnv(chave: String): String? {
    System.getenv(chave)?.takeIf { it.isNotBlank() }?.let { return it }

    val arquivo = rootProject.file(".env")
    if (!arquivo.isFile) return null

    return arquivo.readLines()
        .map { it.trim() }
        .firstOrNull { it.startsWith("$chave=") }
        ?.substringAfter('=')
        ?.trim()
        ?.removeSurrounding("\"")
        ?.removeSurrounding("'")
        ?.takeIf { it.isNotBlank() }
}

/**
 * O IP desta máquina na rede local, para o celular físico saber onde é o servidor.
 *
 * `isSiteLocalAddress` é o que faz o trabalho pesado: ele aceita só as faixas
 * privadas (10/8, 172.16/12, 192.168/16) e por isso descarta sozinho os adaptadores
 * de VPN, que costumam entregar endereços fora delas e são a causa clássica de o
 * app apontar para o lugar errado numa máquina com Radmin, Hamachi ou ZeroTier.
 *
 * Entre os candidatos, 192.168.x.x ganha por ser a faixa que quase todo roteador
 * doméstico usa.
 */
fun ipDaLan(): String? = runCatching {
    NetworkInterface.getNetworkInterfaces()
        .asSequence()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.asSequence() }
        .filterIsInstance<Inet4Address>()
        .filter { it.isSiteLocalAddress }
        .sortedBy { if (it.hostAddress.orEmpty().startsWith("192.168.")) 0 else 1 }
        .firstOrNull()
        ?.hostAddress
}.getOrNull()

/**
 * Onde o servidor mora quando o app roda num aparelho de verdade.
 *
 * O padrão é detectado, não digitado: quem compila é a mesma máquina que roda o
 * `:server`, então o IP dela na LAN é a resposta certa quase sempre. `SERVIDOR_LAN`
 * no `.env` sobrescreve — é a saída para quando o servidor estiver noutro lugar.
 */
val servidorLan: String = doEnv("SERVIDOR_LAN")
    ?: ipDaLan()?.let { "$it:8080" }
    ?: ""

/** Precisa bater com o APP_TOKEN do servidor, e por isso sai da mesma fonte. */
val appToken: String = doEnv("APP_TOKEN") ?: "token-de-teste-local"

android {
    namespace = "com.jean.vocabs"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jean.vocabs"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "SERVIDOR_LAN", "\"$servidorLan\"")
        buildConfigField("String", "APP_TOKEN", "\"$appToken\"")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

dependencies {
    implementation(project(":shared"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    // Modelo latino empacotado: OCR disponível offline desde o primeiro uso.
    implementation(libs.mlkit.text.recognition)
}
