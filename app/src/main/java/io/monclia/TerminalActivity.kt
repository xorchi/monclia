package io.monclia

import android.content.Context
import android.os.Bundle
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

        Logger.init(this)
        Logger.log("App", "Monclia started")

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            Logger.error("Crash", "Uncaught exception", throwable)
            runCatching { File(filesDir, "crash.log").writeText(throwable.stackTraceToString()) }
            runOnUiThread {
                val msg = throwable.stackTraceToString()
                AlertDialog.Builder(this)
                    .setTitle("Crash")
                    .setMessage(msg.take(2000))
                    .setPositiveButton("OK", null)
                    .setNeutralButton("Copy") { _, _ ->
                        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        cb.setPrimaryClip(android.content.ClipData.newPlainText("crash", msg))
                    }
                    .show()
            }
        }

        terminalView = TerminalView(this, null)
        terminalView.setTerminalViewClient(viewClient)
        setContentView(terminalView)
        terminalView.isFocusable = true
        terminalView.isFocusableInTouchMode = true
        terminalView.requestFocus()
        terminalView.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(terminalView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        startWalletCli()
    }

    private fun startWalletCli() {
        val orchestrator = prepareOrchestrator()
        val walletDir = File(filesDir, "wallets").also { it.mkdirs() }

        val session = TerminalSession(
            orchestrator,
            filesDir.absolutePath,
            arrayOf(),
            arrayOf(
                "TERM=xterm-256color",
                "HOME=${filesDir.absolutePath}",
                "WALLET_DIR=${walletDir.absolutePath}",
                "BIN_DIR=${File(filesDir, "bin").absolutePath}",
                "LOG_DIR=${File(filesDir, "logs").absolutePath}",
            ),
            2000,
            this
        )
        terminalSession = session
        terminalView.setTextSize(24)
        terminalView.post { terminalView.attachSession(session) }
    }

    private fun prepareOrchestrator(): String {
        val binDir = File(filesDir, "bin").also { it.mkdirs() }
        val script = File(binDir, "orchestrator.sh")
        val tmp = File(binDir, "orchestrator.sh.tmp")
        assets.open("orchestrator.sh").use { input ->
            tmp.outputStream().use { output -> input.copyTo(output) }
        }
        tmp.renameTo(script)
        script.setExecutable(true)
        return script.absolutePath
    }

    override fun onDestroy() {
        super.onDestroy()
        terminalSession?.finishIfRunning()
    }

    // ── TerminalSessionClient ──────────────────────────────────────────────

    override fun onTextChanged(changedSession: TerminalSession) {
        terminalView.onScreenUpdated()
    }
    override fun onTitleChanged(updatedSession: TerminalSession) {}
    override fun onSessionFinished(finishedSession: TerminalSession) {
        Logger.log("App", "Session finished — closing app")
        finish()
    }
    override fun onCopyTextToClipboard(session: TerminalSession, text: String) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        cb.setPrimaryClip(android.content.ClipData.newPlainText("monclia", text))
    }
    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val cb = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val text = cb.primaryClip?.getItemAt(0)?.text?.toString() ?: return
        session?.write(text)
    }
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
