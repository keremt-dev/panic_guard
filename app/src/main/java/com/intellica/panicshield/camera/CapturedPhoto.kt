package com.intellica.panicshield.camera

import java.io.File

/**
 * A photo captured silently during a panic event, stored in app-private
 * internal storage (never on shared/external media, never uploaded).
 */
data class CapturedPhoto(
    val file: File,
    val capturedAtEpochMs: Long,
)

object CaptureStorage {
    private const val DIR_NAME = "captures"

    fun dir(filesDir: File): File =
        File(filesDir, DIR_NAME).apply { if (!exists()) mkdirs() }

    fun newFile(filesDir: File, timestampMs: Long): File =
        File(dir(filesDir), "panic_$timestampMs.jpg")

    fun list(filesDir: File): List<CapturedPhoto> =
        dir(filesDir)
            .listFiles { f -> f.isFile && f.name.startsWith("panic_") && f.extension == "jpg" }
            ?.map { f ->
                val ts = f.nameWithoutExtension.removePrefix("panic_").toLongOrNull() ?: f.lastModified()
                CapturedPhoto(file = f, capturedAtEpochMs = ts)
            }
            ?.sortedByDescending { it.capturedAtEpochMs }
            ?: emptyList()
}
