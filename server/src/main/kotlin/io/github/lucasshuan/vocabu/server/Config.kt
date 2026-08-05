package io.github.lucasshuan.vocabu.server

import java.io.File

/**
 * Configuration in two layers: environment variable first, local `.env` after.
 *
 * The file exists because retyping two secrets in every new terminal is friction
 * that gets paid daily. The environment variable keeps precedence so CI and
 * production depend on no file.
 *
 * `.env` is in .gitignore. Never commit one.
 */
object Config {

    private val fromFile: Map<String, String> by lazy { readLocalFile() }

    // The takeIf on both sides matters: `APP_TOKEN=` in the file (or exported
    // empty) has to count as absent, otherwise the server comes up with an empty
    // token and accepts any request without a header.
    operator fun get(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: fromFile[key]?.takeIf { it.isNotBlank() }

    fun required(key: String): String = get(key) ?: error(
        "$key is not set. Use one of the two:\n" +
            "  1. \$env:$key = \"value\"   (this terminal session only)\n" +
            "  2. add the line  $key=value  to the .env file at the repository root",
    )

    /**
     * Looks for `.env` walking up from the working directory.
     *
     * The walk is not fussiness: Gradle runs `:server:run` with the working
     * directory in `server/`, not the repository root, so looking only in `.`
     * would find nothing and the file would seem ignored.
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
