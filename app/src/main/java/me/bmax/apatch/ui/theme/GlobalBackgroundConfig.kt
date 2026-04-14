package me.bmax.apatch.ui.theme

import android.content.Context
import android.net.Uri
import java.io.File

object GlobalBackgroundConfig {
    private const val PREFS_NAME = "apatch_global_background"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_URI = "uri"
    private const val FILE_NAME = "global_background"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun getBackgroundUri(context: Context): String? {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_URI, null)
    }

    fun clear(context: Context) {
        clearFiles(context)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_URI, null)
            .putBoolean(KEY_ENABLED, false)
            .apply()
    }

    fun saveBackground(context: Context, sourceUri: Uri): Boolean {
        return try {
            val extension = when (context.contentResolver.getType(sourceUri)?.lowercase()) {
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                "image/gif" -> ".gif"
                else -> ".jpg"
            }

            clearFiles(context)
            val targetFile = File(context.filesDir, "$FILE_NAME$extension")

            context.contentResolver.openInputStream(sourceUri).use { input ->
                if (input == null) return false
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val savedUri = Uri.fromFile(targetFile).buildUpon()
                .appendQueryParameter("t", System.currentTimeMillis().toString())
                .build()
                .toString()

            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_URI, savedUri)
                .putBoolean(KEY_ENABLED, true)
                .apply()

            true
        } catch (_: Exception) {
            false
        }
    }

    private fun clearFiles(context: Context) {
        listOf(".jpg", ".png", ".webp", ".gif").forEach { ext ->
            val file = File(context.filesDir, "$FILE_NAME$ext")
            if (file.exists()) {
                file.delete()
            }
        }
    }
}
