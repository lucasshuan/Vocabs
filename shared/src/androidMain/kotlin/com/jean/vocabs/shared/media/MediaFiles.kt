package com.jean.vocabs.shared.media

import android.content.Context
import java.io.File

/**
 * Onde as fotos e os áudios das capturas ficam.
 *
 * Tudo dentro de `filesDir`: armazenamento privado do app, que não pede
 * permissão nenhuma e some junto com o app se você desinstalar. Coerente com o
 * "local-first" do produto — nada disso aparece na galeria do celular.
 */
object MediaFiles {

    private const val FOLDER = "captures"

    fun newPhoto(context: Context): File = novo(context, "photo", "jpg")

    fun newAudio(context: Context): File = novo(context, "audio", "wav")

    private fun novo(context: Context, prefixo: String, extensao: String): File {
        val folder = File(context.filesDir, FOLDER).apply { mkdirs() }
        return File(folder, "$prefixo-${System.currentTimeMillis()}.$extensao")
    }

    /** Silencioso de propósito: se o file já não existe, o objetivo foi atingido. */
    fun remover(path: String) {
        runCatching { File(path).delete() }
    }
}
