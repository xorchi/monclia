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
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream

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

    private fun writeCrashToDownloads(msg: String) {
        try {
            val dl = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS)
            java.io.File(dl, "monclia-crash.txt").writeText(msg)
        } catch (_: Throwable) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try { onCreateSafe() } catch (e: Throwable) { writeCrashToDownloads(e.stackTraceToString()); throw e }
    }

    private fun onCreateSafe() {
        Logger.init(this)
        Logger.log("App", "Monclia started")

        Thread.setDefaultUncaughtExceptionHandler { _, throwable ->
            val msg = throwable.stackTraceToString()
            // Write to Downloads so user can read without root
            try {
                val dl = android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS)
                java.io.File(dl, "monclia-crash.txt").writeText(msg)
            } catch (_: Throwable) {}
            runOnUiThread {
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
        terminalView.setTextSize(24)
        terminalView.post {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.showSoftInput(terminalView, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT)
        }

        startWalletCli()
    }

    private fun startWalletCli() {
        val walletDir = File(filesDir, "wallets").also { it.mkdirs() }
        val binDir   = File(filesDir, "bin").also { it.mkdirs() }
        val logDir   = File(filesDir, "logs").also { it.mkdirs() }
        val walletCli = File(binDir, "monero-wallet-cli")

        if (walletCli.exists()) {
            spawnSession(walletDir, binDir, logDir)
        } else {
            downloadWalletCli(walletCli, binDir) {
                runOnUiThread { spawnSession(walletDir, binDir, logDir) }
            }
        }
    }

    private fun spawnSession(walletDir: File, binDir: File, logDir: File) {
        val scriptContent = assets.open("orchestrator.sh").bufferedReader().readText()
        val session = TerminalSession(
            "/system/bin/sh",
            filesDir.absolutePath,
            arrayOf("-c", scriptContent),
            arrayOf(
                "TERM=xterm-256color",
                "HOME=${filesDir.absolutePath}",
                "WALLET_DIR=${walletDir.absolutePath}",
                "BIN_DIR=${binDir.absolutePath}",
                "LOG_DIR=${logDir.absolutePath}",
            ),
            2000,
            this
        )
        terminalSession = session
        terminalView.post { terminalView.attachSession(session) }
    }

    private fun downloadWalletCli(dest: File, binDir: File, onComplete: () -> Unit) {
        val url = "https://downloads.getmonero.org/cli/monero-android-armv7-v0.18.4.5.tar.bz2"
        val expectedSha256 = "3cd6611c5c33ae4c10e52698826560bbb17e00cf2f8a2d7f61e79d28f0f36ef6"
        val archive = File(binDir, "monero-android-armv7.tar.bz2")

        val progressSession = TerminalSession(
            "/system/bin/sh",
            filesDir.absolutePath,
            arrayOf("-c", "echo 'Downloading monero-wallet-cli...'; echo 'This may take a moment.'"),
            arrayOf("TERM=xterm-256color"),
            200,
            this
        )
        terminalSession = progressSession
        runOnUiThread { terminalView.attachSession(progressSession) }

        Thread {
            try {
                Logger.log("App", "Downloading monero-wallet-cli")
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.connect()
                val total = conn.contentLengthLong
                var downloaded = 0L

                archive.outputStream().use { out ->
                    conn.inputStream.use { input ->
                        val buf = ByteArray(32768)
                        var n: Int
                        while (input.read(buf).also { n = it } != -1) {
                            out.write(buf, 0, n)
                            downloaded += n
                            if (total > 0) {
                                val pct = downloaded * 100 / total
                                Logger.log("App", "Download $pct%")
                            }
                        }
                    }
                }

                Logger.log("App", "Verifying SHA256")
                val md = MessageDigest.getInstance("SHA-256")
                archive.inputStream().use { md.update(it.readBytes()) }
                val actualSha256 = md.digest().joinToString("") { "%02x".format(it) }

                if (actualSha256 != expectedSha256) {
                    Logger.error("App", "SHA256 mismatch: $actualSha256")
                    archive.delete()
                    return@Thread
                }

                Logger.log("App", "Extracting archive")
                extractBz2Tar(archive, binDir, "monero-wallet-cli")
                archive.delete()

                if (dest.exists()) {
                    dest.setExecutable(true)
                    Logger.log("App", "monero-wallet-cli ready")
                    onComplete()
                } else {
                    Logger.error("App", "Binary not found after extraction")
                }

            } catch (e: Throwable) {
                Logger.error("App", "Download failed", e)
            }
        }.start()
    }

    private fun extractBz2Tar(archive: File, destDir: File, targetFilename: String) {
        archive.inputStream().buffered().use { fis ->
            BZip2CompressorInputStream(fis).use { bz2 ->
                TarArchiveInputStream(bz2).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val name = File(entry.name).name
                        if (name == targetFilename) {
                            val dest = File(destDir, name)
                            dest.outputStream().use { tar.copyTo(it) }
                            Logger.log("App", "Extracted: ${dest.absolutePath}")
                            return
                        }
                        entry = tar.nextEntry
                    }
                }
            }
        }
        throw Exception("$targetFilename not found in archive")
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
        if (finishedSession == terminalSession) {
            Logger.log("App", "Session finished — closing app")
            finish()
        }
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
