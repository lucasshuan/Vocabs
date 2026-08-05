import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

kotlin {
    compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

dependencies {
    implementation(project(":contracts"))

    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)

    implementation(libs.anthropic.java)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("com.jean.vocabs.server.ApplicationKt")
}

// Desde o Java 18, System.out/err usam stdout.encoding/stderr.encoding, que sao
// independentes de file.encoding e caem na codificacao nativa do SO. No Windows
// isso faz a JVM filha escrever em cp1252, o Gradle reler como UTF-8 e os acentos
// virarem U+FFFD no caminho. As tres precisam estar alinhadas em UTF-8.
tasks.withType<JavaExec>().configureEach {
    defaultCharacterEncoding = "UTF-8"
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")

    // Lets a parallel instance start without touching .env:
    //   .\gradlew.bat :server:run -PMODEL=claude-haiku-4-5 -PPORT=8081
    // Goes through a Gradle property rather than an environment variable because
    // the daemon has its own environment — exporting in the shell would not reach here.
    listOf("MODEL", "PORT").forEach { key ->
        providers.gradleProperty(key).orNull?.let { environment(key, it) }
    }
}
