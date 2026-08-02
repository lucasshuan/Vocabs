plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

// Dois targets porque este módulo é consumido pelos dois lados da conversa:
// android -> :shared (app)   |   jvm -> :server
// É isso que faz o compilador reclamar nos dois lados quando o formato da ficha muda.
kotlin {
    android {
        namespace = "com.jean.vocabs.contracts"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }
    jvm()

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.serialization.json)
        }
    }
}
