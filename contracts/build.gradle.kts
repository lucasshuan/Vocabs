plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.kotlin.serialization)
}

// Two targets, one per side: android -> :shared, jvm -> :server. That is what
// makes both sides fail to compile when a DTO changes shape.
kotlin {
    android {
        namespace = "io.github.lucasshuan.vocabu.contracts"
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
