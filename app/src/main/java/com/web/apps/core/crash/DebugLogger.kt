package com.web.apps.core.crash

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    private const val RELATIVE_PATH = "Download/WebApps/debug_logs"
    private const val FILE_NAME = "debug.txt"

    fun log(context: Context, tag: String, message: String) {
        try {
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())
            val line = "[$timestamp] [$tag] $message\n"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val uri = findOrCreateFile(resolver)
                resolver.openOutputStream(uri, "wa")?.use { it.write(line.toByteArray()) }
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "WebApps/debug_logs")
                if (!dir.exists()) dir.mkdirs()
                File(dir, FILE_NAME).appendText(line)
            }
        } catch (e: Exception) {
            // ignore, jangan sampai logging sendiri menyebabkan crash
        }
    }

    @androidx.annotation.RequiresApi(android.os.Build.VERSION_CODES.Q)
    private fun findOrCreateFile(resolver: android.content.ContentResolver): Uri {
        val projection = arrayOf(MediaStore.Downloads._ID)
        val selection = "${MediaStore.Downloads.DISPLAY_NAME}=? AND ${MediaStore.Downloads.RELATIVE_PATH}=?"
        val selectionArgs = arrayOf(FILE_NAME, "$RELATIVE_PATH/")

        resolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val id = cursor.getLong(0)
                return ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id)
            }
        }

        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, FILE_NAME)
            put(MediaStore.Downloads.MIME_TYPE, "text/plain")
            put(MediaStore.Downloads.RELATIVE_PATH, RELATIVE_PATH)
        }
        return resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)!!
    }
}