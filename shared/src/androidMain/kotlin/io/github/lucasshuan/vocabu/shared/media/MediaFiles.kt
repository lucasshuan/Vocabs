package io.github.lucasshuan.vocabu.shared.media

import android.content.Context
import java.io.File

/**
 * `filesDir`: app-private, no permission needed, and nothing reaches the phone's
 * gallery.
 */
object MediaFiles {

    private const val FOLDER = "captures"

    fun newPhoto(context: Context): File = new(context, "photo", "jpg")

    fun newAudio(context: Context): File = new(context, "audio", "wav")

    private fun new(context: Context, prefix: String, extension: String): File {
        val folder = File(context.filesDir, FOLDER).apply { mkdirs() }
        return File(folder, "$prefix-${System.currentTimeMillis()}.$extension")
    }

    /** Silent on purpose: if the file is already gone, the goal was met. */
    fun remove(path: String) {
        runCatching { File(path).delete() }
    }
}
