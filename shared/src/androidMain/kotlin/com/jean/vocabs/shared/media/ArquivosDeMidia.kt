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
object ArquivosDeMidia {

    private const val PASTA = "captures"

    fun novaFoto(context: Context): File = novo(context, "photo", "jpg")

    fun novoAudio(context: Context): File = novo(context, "audio", "wav")

    private fun novo(context: Context, prefixo: String, extensao: String): File {
        val pasta = File(context.filesDir, PASTA).apply { mkdirs() }
        return File(pasta, "$prefixo-${System.currentTimeMillis()}.$extensao")
    }

    /** Silencioso de propósito: se o arquivo já não existe, o objetivo foi atingido. */
    fun remover(caminho: String) {
        runCatching { File(caminho).delete() }
    }
}
