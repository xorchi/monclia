package io.monclia

import android.app.Application
import android.os.Environment
import java.io.File

class MoncLiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            try {
                val msg = throwable.stackTraceToString()
                // Write to app-specific external storage (no permission needed)
                val extDir = getExternalFilesDir(null)
                if (extDir != null) {
                    File(extDir, "crash.txt").writeText(msg)
                }
                // Also try Downloads
                val dl = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                File(dl, "monclia-crash.txt").writeText(msg)
            } catch (_: Throwable) {}
        }
    }
}
