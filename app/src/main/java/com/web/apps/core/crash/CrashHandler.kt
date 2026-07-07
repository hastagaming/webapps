package com.web.apps.core.crash

import android.content.Context
import android.os.Build
import java.io.File
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(t: Thread, e: Throwable) {
        saveCrashToFile(e)
        defaultHandler?.uncaughtException(t, e)
    }

    private fun saveCrashToFile(e: Throwable) {
        try {
            val crashDir = File(context.getExternalFilesDir(null), "crashes")
            if (!crashDir.exists()) {
                crashDir.mkdirs()
            }

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val crashFile = File(crashDir, "crash_$timestamp.txt")

            PrintWriter(crashFile.outputStream()).use { writer ->
                writer.println("=== WebApps Crash Report ===")
                writer.println("Time: $timestamp")
                writer.println("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
                writer.println("Android: ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})")
                writer.println("")
                writer.println("Exception: ${e.javaClass.name}")
                writer.println("Message: ${e.message}")
                writer.println("")
                writer.println("Stack Trace:")
                e.printStackTrace(writer)
                writer.println("")

                var cause = e.cause
                var level = 1
                while (cause != null) {
                    writer.println("Caused by ($level): ${cause.javaClass.name}")
                    writer.println("Message: ${cause.message}")
                    cause.printStackTrace(writer)
                    cause = cause.cause
                    level++
                }
            }
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}