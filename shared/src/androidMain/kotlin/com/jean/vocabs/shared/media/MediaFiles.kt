package com.jean.vocabs.shared.media

import android.content.Context
import java.io.File

/**
 * Where captured photos and audio live.
 *
 * All inside `filesDir`: app-private storage, which needs no permission and goes
 * away with the app. Consistent with the product being local-first — none of it
 * appears in the phone's gallery.
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
