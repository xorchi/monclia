package io.monclia

import android.content.ComponentName
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.IBinder
import android.provider.MediaStore
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import java.io.File

class TerminalActivity : AppCompatActivity(), TerminalSessionClient {

    private lateinit var terminalView: TerminalView
    private var terminalSession: TerminalSession? = null
    private var walletService: WalletService? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            walletService = (binder as WalletService.LocalBinder).getService()
            try {
                startWalletCli()
            } catch (e: Exception) {
                File(filesDir, "crash.log").writeText(e.stackTraceToString())
                writeLogToDownloads("monclia-crash.log", e.stackTraceToString())
                val msg = e.stackTraceToString()
                AlertDialog.Builder(this@TerminalActivity)
                    .setTitle("Startup Error")
                    .setMessage(msg.take(2000))
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Copy") { _, _ ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("crash", msg))
                    }
                    .show()
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            walletService = null
        }
    }

    private val viewClient = object : TerminalViewClient {
        override fun onScale(scale: Float): Float = scale
        override fun onSingleTapUp(e: MotionEvent) {}
        override fun shouldBackButtonBeMappedToEscape(): Boolean = false
        override fun shouldEnforceCharBasedInput(): Boolean = true
        override fun shouldUseCtrlSpaceWorkaround(): Boolean = false
        override fun isTerminalViewSelected(): Boolean = true
        override fun copyModeChanged(copyMode: Boolean) {}
        override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession): Boolean = false
        override fun onKeyUp(keyCode: Int, e: KeyEvent): Boolean = false
        override fun onLongPress(event: MotionEvent): Boolean = false
        override fun readControlKey(): Boolean = false
        override fun readAltKey(): Boolean = false
        override fun readShiftKey(): Boolean = false
        override fun readFnKey(): Boolean = false
        override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession): Boolean = false
        override fun onEmulatorSet() {}
        override fun logError(tag: String, message: String) {}
        override fun logWarn(tag: String, message: String) {}
        override fun logInfo(tag: String, message: String) {}
        override fun logDebug(tag: String, message: String) {}
        override fun logVerbose(tag: String, message: String) {}
        override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
        override fun logStackTrace(tag: String, e: Exception) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Log.e("Monclia", throwable.stackTraceToString())
            runCatching {
                File(filesDir, "crash.log").writeText(throwable.stackTraceToString())
                writeLogToDownloads("monclia-crash.log", throwable.stackTraceToString())
            }
            runOnUiThread {
                val msg = throwable.stackTraceToString()
                AlertDialog.Builder(this)
                    .setTitle("Crash")
                    .setMessage(msg.take(2000))
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Copy") { _, _ ->
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        clipboard.setPrimaryClip(android.content.ClipData.newPlainText("crash", msg))
                    }
                    .show()
            }
        }

        terminalView = TerminalView(this, null)
        terminalView.setTerminalViewClient(viewClient)
        setContentView(terminalView)

        val intent = Intent(this, WalletService::class.java)
        startForegroundService(intent)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private fun startWalletCli() {
        val walletDir = File(filesDir, "wallets").also { it.mkdirs() }
        val stubScript = prepareStubScript()

        val session = TerminalSession(
            "/system/bin/sh",
            walletDir.absolutePath,
            arrayOf(stubScript),
            arrayOf("TERM=xterm-256color", "HOME=${filesDir.absolutePath}"),
            2000,
            this
        )
        terminalSession = session
        terminalView.setTextSize(24)
        terminalView.post { terminalView.attachSession(session) }
    }

    private fun prepareStubScript(): String {
        val binDir = File(filesDir, "bin").also { it.mkdirs() }
        val stub = File(binDir, "wallet-stub.sh")
        if (!stub.exists()) {
            assets.open("wallet-stub.sh").use { input ->
                stub.outputStream().use { output -> input.copyTo(output) }
            }
            stub.setExecutable(true)
        }
        return stub.absolutePath
    }

    private fun writeLogToDownloads(filename: String, content: String) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, filename)
                    put(MediaStore.Downloads.MIME_TYPE, "text/plain")
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(
                    MediaStore.Downloads.EXTERNAL_CONTENT_URI, values
                )
                uri?.let {
                    contentResolver.openOutputStream(it)?.use { os ->
                        os.write(content.toByteArray())
                    }
                    values.clear()
                    values.put(MediaStore.Downloads.IS_PENDING, 0)
                    contentResolver.update(it, values, null, null)
                }
            } else {
                val dir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )
                File(dir, filename).writeText(content)
            }
        } catch (e: Exception) {
            Log.e("Monclia", "Failed to write log: ${e.message}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindService(serviceConnection)
        terminalSession?.finishIfRunning()
    }

    // ── TerminalSessionClient ──────────────────────────────────────────────

    override fun onTextChanged(changedSession: TerminalSession) {
        terminalView.onScreenUpdated()
    }
    override fun onTitleChanged(updatedSession: TerminalSession) {}
    override fun onSessionFinished(finishedSession: TerminalSession) {}
    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {}
    override fun onPasteTextFromClipboard(session: TerminalSession?) {}
    override fun onBell(session: TerminalSession) {}
    override fun onColorsChanged(session: TerminalSession) {}
    override fun onTerminalCursorStateChange(state: Boolean) {}
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) {}
    override fun getTerminalCursorStyle(): Int? = null
    override fun logError(tag: String, message: String) {}
    override fun logWarn(tag: String, message: String) {}
    override fun logInfo(tag: String, message: String) {}
    override fun logDebug(tag: String, message: String) {}
    override fun logVerbose(tag: String, message: String) {}
    override fun logStackTraceWithMessage(tag: String, message: String, e: Exception) {}
    override fun logStackTrace(tag: String, e: Exception) {}
}
