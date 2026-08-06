package io.github.lucasshuan.vocabu.server

import java.io.File

/**
 * Environment variable first, local `.env` after — so CI and production depend
 * on no file. `.env` is gitignored. Never commit one.
 */
object Config {

    private val fromFile: Map<String, String> by lazy { readLocalFile() }

    // Blank counts as absent on both sides: `APP_TOKEN=` would otherwise bring
    // the server up with an empty token, accepting any request without a header.
    operator fun get(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: fromFile[key]?.takeIf { it.isNotBlank() }

    fun required(key: String): String = get(key) ?: error(
        "$key is not set. Use one of the two:\n" +
            "  1. \$env:$key = \"value\"   (this terminal session only)\n" +
            "  2. add the line  $key=value  to the .env file at the repository root",
    )

    /**
     * Walks up rather than reading `.`: Gradle runs `:server:run` from `server/`,
     * not the repository root, so `.env` would seem ignored.
     */
    private fun readLocalFile(): Map<String, String> {
        val file = generateSequence(File("").absoluteFile) { it.parentFile }
            .map { File(it, ".env") }
            .firstOrNull { it.isFile }
            ?: return emptyMap()

        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") && it.contains('=') }
            .associate { row ->
                val key = row.substringBefore('=').trim()
                val value = row.substringAfter('=').trim().removeSurrounding("\"").removeSurrounding("'")
                key to value
            }
    }
}
