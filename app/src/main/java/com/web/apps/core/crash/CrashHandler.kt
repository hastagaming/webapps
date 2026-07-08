package com.web.apps.core.crash

import android.content.ContentValues
import android.content.Context
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        saveCrashReport(e)
        defaultHandler?.uncaughtException(t, e)
    }

    private fun saveCrashReport(e: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val fileName = "crash_$timestamp.txt"

            val content = buildString {
                appendLine("=== WebApps Crash Report ===")
                appendLine("Time: $timestamp")
                appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                appendLine("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                appendLine()
                appendLine("Exception: ${e.javaClass.name}")
                appendLine("Message: ${e.message}")
                appendLine()
                appendLine("Stack Trace:")
                appendLine(e.stackTraceToString())

                var cause = e.cause
                var level = 1
                while (cause != null) {
                    appendLine()
                    appendLine("Caused by ($level): ${cause.javaClass.name}")
                    appendLine("Message: ${cause.message}")
                    appendLine(cause.stackTraceToString())
                    cause = cause.cause
                    level++
                }
            }

            writeToPublicDownloads(fileName, content, "WebApps/crashes")
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }

    companion object {
        fun writeToPublicDownloads(fileName: String, content: String, subFolder: String) {
            try {
                val context = com.web.apps.WebAppsApplication.appContextInstance ?: return
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                        put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                        put(MediaStore.Downloads.RELATIVE_PATH, "Download/$subFolder")
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { stream -> stream.write(content.toByteArray()) }
                    }
                } else {
                    val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), subFolder)
                    if (!dir.exists()) dir.mkdirs()
                    val file = File(dir, fileName)
                    PrintWriter(file.outputStream()).use { it.print(content) }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }
}