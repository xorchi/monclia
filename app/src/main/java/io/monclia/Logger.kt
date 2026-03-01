package io.monclia

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Logger {

    private var logFile: File? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    fun init(context: Context) {
        val logDir = File(context.filesDir, "logs").also { it.mkdirs() }
        logFile = File(logDir, "monclia.log")
    }

    fun log(tag: String, message: String) {
        val timestamp = dateFormat.format(Date())
        val line = "[$timestamp] [$tag] $message\n"
        android.util.Log.i("Monclia/$tag", message)
        logFile?.appendText(line)
    }

    fun error(tag: String, message: String, throwable: Throwable? = null) {
        val timestamp = dateFormat.format(Date())
        val trace = throwable?.stackTraceToString()?.let { "\n$it" } ?: ""
        val line = "[$timestamp] [ERROR/$tag] $message$trace\n"
        android.util.Log.e("Monclia/$tag", message, throwable)
        logFile?.appendText(line)
    }

    fun getLogPath(): String = logFile?.absolutePath ?: "not initialized"

    fun getLogContent(): String = logFile?.readText() ?: "no log"

    fun clear() = logFile?.writeText("")
}
