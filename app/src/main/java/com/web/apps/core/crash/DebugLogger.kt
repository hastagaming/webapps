package com.web.apps.core.crash

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DebugLogger {
    fun log(context: Context, tag: String, message: String) {
        try {
            val logDir = File(context.getExternalFilesDir(null), "debug_logs")
            if (!logDir.exists()) logDir.mkdirs()

            val logFile = File(logDir, "debug.txt")
            val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())

            logFile.appendText("[$timestamp] [$tag] $message\n")
        } catch (e: Exception) {
            // ignore
        }
    }
}