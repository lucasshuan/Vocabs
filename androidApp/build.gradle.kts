import java.net.Inet4Address
import java.net.NetworkInterface
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 has Kotlin built in: applying 'org.jetbrains.kotlin.android' here is an error.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/**
 * The server's `Config` semantics, resolved at build time: a phone cannot read
 * the `.env` on your machine, so the value is baked into the APK.
 */
fun fromEnv(key: String): String? {
    System.getenv(key)?.takeIf { it.isNotBlank() }?.let { return it }

    val file = rootProject.file(".env")
    if (!file.isFile) return null

    return file.readLines()
        .map { it.trim() }
        .firstOrNull { it.startsWith("$key=") }
        ?.substringAfter('=')
        ?.trim()
        ?.removeSurrounding("\"")
        ?.removeSurrounding("'")
        ?.takeIf { it.isNotBlank() }
}

/**
 * `isSiteLocalAddress` accepts only 10/8, 172.16/12 and 192.168/16, which
 * discards VPN adapters on its own — they hand out addresses outside those
 * ranges, and are the classic reason the app points at the wrong place on a
 * machine running Radmin, Hamachi or ZeroTier.
 *
 * 192.168.x.x wins among candidates: nearly every home router uses it.
 */
fun lanAddress(): String? = runCatching {
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
 * Detected, not typed: whoever builds usually also runs `:server`. `SERVER_LAN`
 * in `.env` overrides, for when it is elsewhere.
 */
val lanServer: String = fromEnv("SERVER_LAN")
    ?: lanAddress()?.let { "$it:8080" }
    ?: ""

/** Must match the server's APP_TOKEN, hence the same source. */
val appToken: String = fromEnv("APP_TOKEN") ?: "local-test-token"

android {
    namespace = "io.github.lucasshuan.vocabu"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "io.github.lucasshuan.vocabu"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "LAN_SERVER", "\"$lanServer\"")
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

    // Resources have no compiler: an untranslated key, or a `%1$d` that became
    // `%1$s`, otherwise shows up on a device, in the language you don't run.
    lint {
        error += listOf(
            "MissingTranslation",
            "ImpliedQuantity",
            "MissingQuantity",
            "StringFormatInvalid",
            "StringFormatMatches",
        )
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
    // Latin model bundled: OCR works offline from first launch.
    implementation(libs.mlkit.text.recognition)

    // The -junit artifact, not plain kotlin-test: `kotlin.test.Test` is a
    // typealias that resolves only with a framework binding on the classpath.
    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.junit)
}
