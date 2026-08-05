package com.jean.vocabs.server

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

    private val doArquivo: Map<String, String> by lazy { readLocalFile() }

    // The takeIf on both sides matters: `APP_TOKEN=` in the file (or exported
    // empty) has to count as absent, otherwise the server comes up with an empty
    // token and accepts any request without a header.
    operator fun get(key: String): String? =
        System.getenv(key)?.takeIf { it.isNotBlank() }
            ?: doArquivo[key]?.takeIf { it.isNotBlank() }

    fun obrigatorio(key: String): String = get(key) ?: error(
        "$key não definida. Use uma das duas opções:\n" +
            "  1. \$env:$key = \"value\"   (só nesta sessão do terminal)\n" +
            "  2. adicione a row  $key=value  no file .env na raiz do projeto",
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
