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
    mainClass.set("io.github.lucasshuan.vocabu.server.ApplicationKt")
}

// Since Java 18, System.out/err follow stdout.encoding/stderr.encoding, which
// are independent of file.encoding and default to the OS encoding. On Windows
// the child JVM then writes cp1252, Gradle reads UTF-8, and accents arrive as
// U+FFFD. All three have to be aligned.
tasks.withType<JavaExec>().configureEach {
    defaultCharacterEncoding = "UTF-8"
    jvmArgs("-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")

    // A parallel instance without touching .env:
    //   .\gradlew.bat :server:run -PMODEL=claude-opus-5 -PPORT=8081
    // A Gradle property, not an environment variable: the daemon has its own
    // environment, so exporting in the shell would not reach here.
    listOf("MODEL", "PORT").forEach { key ->
        providers.gradleProperty(key).orNull?.let { environment(key, it) }
    }
}
