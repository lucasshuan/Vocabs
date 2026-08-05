package com.jean.vocabs.ui.settings

import android.content.Context
import com.jean.vocabs.shared.domain.ExportData
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

object VocabuExporter {
    suspend fun create(context: Context, data: ExportData): File = withContext(Dispatchers.IO) {
        val folder = File(context.cacheDir, "exports").apply { mkdirs() }
        val destination = File(folder, "Vocabu-${System.currentTimeMillis()}.zip")
        ZipOutputStream(FileOutputStream(destination)).use { zip ->
            zip.putNextEntry(ZipEntry("Vocabu.json"))
            zip.write(json(data).toString(2).toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            data.captures.distinctBy { it.mediaPath }.forEach { capture ->
                val path = capture.mediaPath ?: return@forEach
                val file = File(path).takeIf(File::isFile) ?: return@forEach
                zip.putNextEntry(ZipEntry("media/${capture.id}-${file.name}"))
                FileInputStream(file).use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        destination
    }

    private fun json(data: ExportData) = JSONObject().apply {
        // 2: `ipa` virou `pronuncia` e o par de idiomas deixou de ser um valor
        // fixo do arquivo para ser uma propriedade de cada captura.
        put("schemaVersion", 3)
        put("app", "Vocabu")
        put("exportedAt", System.currentTimeMillis())
        put("aiUsage", JSONObject().put("month", data.aiUsage.month).put("generations", data.aiUsage.used))
        put("captures", JSONArray().apply {
            data.captures.forEach { capture -> put(JSONObject().apply {
                put("id", capture.id)
                put("snippet", capture.snippet)
                put("source", capture.source)
                put("createdAt", capture.createdAt)
                put("status", capture.status.name)
                put("format", capture.format.name)
                put("media", capture.mediaPath?.let { "media/${capture.id}-${File(it).name}" })
                put("durationMs", capture.durationMs)
                put("transcriptionError", capture.transcriptionError)
                put("nativeLanguage", capture.languagePair.native)
                put("targetLanguage", capture.languagePair.target)
            }) }
        })
        put("entries", JSONArray().apply {
            data.entries.forEach { entry -> put(JSONObject().apply {
                put("id", entry.id)
                put("captureId", entry.captureId)
                put("target", entry.target)
                put("startIndex", entry.start)
                put("endIndex", entry.end)
                put("type", entry.type.name)
                put("status", entry.status.name)
                put("translation", entry.card?.translation)
                put("definitions", JSONArray(entry.card?.definitions.orEmpty()))
                put("example", entry.card?.example)
                put("pronunciation", entry.card?.pronunciation)
                put("related", JSONArray(entry.card?.related.orEmpty()))
                put("errorCode", entry.errorCode?.name)
                put("errorDetail", entry.errorDetail)
                put("retention", entry.retention?.let { retention -> JSONObject()
                    .put("points", retention.points)
                    .put("decayRate", retention.decayRate)
                    .put("lastInteractionAt", retention.lastInteraction)
                    .put("reviews", retention.reviews)
                    .put("correctCount", retention.hits)
                    .put("incorrectCount", retention.misses)
                })
            }) }
        })
        put("activity", JSONArray().apply {
            data.activity.forEach { put(JSONObject().put("day", it.day).put("reviews", it.reviews)) }
        })
    }
}
