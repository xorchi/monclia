package io.monclia

import android.app.Application

class MoncLiaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val msg = throwable.stackTraceToString()
            android.os.Handler(android.os.Looper.getMainLooper()).post {
                android.widget.Toast.makeText(this, msg.take(500), android.widget.Toast.LENGTH_LONG).show()
            }
            Thread.sleep(5000)
        }
    }
}
