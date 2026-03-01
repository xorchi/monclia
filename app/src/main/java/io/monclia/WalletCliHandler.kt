package io.monclia

import android.content.Context
import android.os.Environment
import com.termux.terminal.TerminalSession
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class WalletCliHandler(
    private val context: Context,
    private val session: TerminalSession
) {
    private val filesDir = context.filesDir
    private val walletDir = File(filesDir, "wallets").also { it.mkdirs() }
    private val defaultWalletPath = File(walletDir, "main-wallet").absolutePath
    private val defaultNode = "node.sethforprivacy.com:18089"

    private var state: State = State.IDLE
    private var walletHandle: Long = 0L
    private var pendingInput = StringBuilder()
    private var newWalletName = ""
    private var newWalletPassword = ""
    private var seedPhrase = ""
    private var sendAddress = ""

    enum class State {
        IDLE,
        MAIN_MENU,
        AWAIT_PASSWORD,
        AWAIT_NEW_WALLET_NAME,
        AWAIT_NEW_WALLET_PASSWORD,
        AWAIT_NEW_WALLET_PASSWORD_CONFIRM,
        AWAIT_SEED,
        AWAIT_RESTORE_HEIGHT,
        AWAIT_ZIP_PATH,
        DASHBOARD,
        AWAIT_SEND_ADDRESS,
        AWAIT_SEND_AMOUNT,
    }

    // ── Output helpers ─────────────────────────────────────────────────────

    private fun write(text: String) = session.write(text)
    private fun writeln(text: String = "") = session.write("$text\r\n")
    private fun clear() = write("\u001b[2J\u001b[H")

    private fun header() {
        writeln("╔══════════════════════════════════════╗")
        writeln("║        Monclia  v0.1.0               ║")
        writeln("║  Private Monero Wallet for Android   ║")
        writeln("╚══════════════════════════════════════╝")
        writeln()
    }

    private fun separator() = writeln("─".repeat(42))

    // ── Lifecycle ──────────────────────────────────────────────────────────

    fun start() {
        Logger.log("CLI", "WalletCliHandler started")
        clear()
        header()

        val walletExists = try {
            WalletJni.walletExists(defaultWalletPath)
        } catch (e: Throwable) {
            Logger.error("CLI", "WalletJni not available", e)
            writeln("ERROR: Native wallet library failed to load.")
            writeln(e.message ?: "Unknown error")
            return
        }

        if (walletExists) {
            promptPassword()
        } else {
            showMainMenu()
        }
    }

    fun onInput(input: String) {
        pendingInput.append(input)
        val line = pendingInput.toString()
        if (!line.contains('\n') && !line.contains('\r')) return
        val trimmed = line.trim()
        pendingInput.clear()

        Logger.log("CLI", "Input: state=$state value='$trimmed'")

        when (state) {
            State.MAIN_MENU                      -> handleMainMenu(trimmed)
            State.AWAIT_PASSWORD                 -> handlePassword(trimmed)
            State.AWAIT_NEW_WALLET_NAME          -> handleNewWalletName(trimmed)
            State.AWAIT_NEW_WALLET_PASSWORD      -> handleNewWalletPassword(trimmed)
            State.AWAIT_NEW_WALLET_PASSWORD_CONFIRM -> handleNewWalletPasswordConfirm(trimmed)
            State.AWAIT_SEED                     -> handleSeed(trimmed)
            State.AWAIT_RESTORE_HEIGHT           -> handleRestoreHeight(trimmed)
            State.AWAIT_ZIP_PATH                 -> handleZipPath(trimmed)
            State.DASHBOARD                      -> handleDashboard(trimmed)
            State.AWAIT_SEND_ADDRESS             -> handleSendAddress(trimmed)
            State.AWAIT_SEND_AMOUNT              -> handleSendAmount(trimmed)
            else -> {}
        }
    }

    // ── Main Menu ──────────────────────────────────────────────────────────

    private fun showMainMenu() {
        writeln("No wallet found.")
        writeln()
        writeln("  [1] Create new wallet")
        writeln("  [2] Restore from seed phrase")
        writeln("  [3] Restore from backup file (zip)")
        writeln("  [4] Exit")
        writeln()
        write("Select: ")
        state = State.MAIN_MENU
    }

    private fun handleMainMenu(input: String) {
        when (input) {
            "1"  -> promptNewWalletName()
            "2"  -> promptSeed()
            "3"  -> promptZipPath()
            "4"  -> { writeln("\nGoodbye."); session.finishIfRunning() }
            else -> { writeln("\nInvalid option."); showMainMenu() }
        }
    }

    // ── Create Wallet ──────────────────────────────────────────────────────

    private fun promptNewWalletName() {
        writeln()
        write("Wallet name (default: main-wallet): ")
        state = State.AWAIT_NEW_WALLET_NAME
    }

    private fun handleNewWalletName(input: String) {
        newWalletName = if (input.isEmpty()) "main-wallet" else input.trim()
        writeln()
        write("Wallet password: ")
        state = State.AWAIT_NEW_WALLET_PASSWORD
    }

    private fun handleNewWalletPassword(input: String) {
        newWalletPassword = input
        writeln()
        write("Confirm password: ")
        state = State.AWAIT_NEW_WALLET_PASSWORD_CONFIRM
    }

    private fun handleNewWalletPasswordConfirm(input: String) {
        if (input != newWalletPassword) {
            writeln("\nPasswords do not match. Try again.")
            write("Wallet password: ")
            state = State.AWAIT_NEW_WALLET_PASSWORD
            return
        }
        createWallet(newWalletName, newWalletPassword)
    }

    private fun createWallet(name: String, password: String) {
        writeln("\nCreating wallet...")
        val path = File(walletDir, name).absolutePath
        Thread {
            try {
                val handle = WalletJni.createWallet(path, password, "English")
                if (handle == 0L) {
                    writeln("ERROR: Failed to create wallet.")
                    showMainMenu()
                    return@Thread
                }
                walletHandle = handle
                Logger.log("CLI", "Wallet created: $path")
                val seed = WalletJni.getSeed(handle)
                writeln()
                writeln("Wallet created successfully!")
                writeln()
                writeln("SEED PHRASE — write this down and keep it safe:")
                separator()
                writeln(seed)
                separator()
                writeln()
                connectAndShowDashboard()
            } catch (e: Throwable) {
                Logger.error("CLI", "createWallet failed", e)
                writeln("ERROR: ${e.message}")
                showMainMenu()
            }
        }.start()
    }

    // ── Restore from seed ──────────────────────────────────────────────────

    private fun promptSeed() {
        writeln()
        writeln("Enter your 25-word seed phrase:")
        write("> ")
        state = State.AWAIT_SEED
    }

    private fun handleSeed(input: String) {
        seedPhrase = input.trim()
        val wordCount = seedPhrase.split(" ").size
        if (wordCount != 25) {
            writeln("\nSeed must be 25 words (found: $wordCount). Try again.")
            promptSeed()
            return
        }
        writeln()
        write("Restore height (0 to scan from genesis): ")
        state = State.AWAIT_RESTORE_HEIGHT
    }

    private fun handleRestoreHeight(input: String) {
        val height = input.trim().toLongOrNull() ?: 0L
        writeln("\nRestoring wallet from seed...")
        val path = File(walletDir, "restored-wallet").absolutePath
        Thread {
            try {
                val handle = WalletJni.recoveryWallet(path, "", seedPhrase, height)
                if (handle == 0L) {
                    writeln("ERROR: Failed to restore wallet.")
                    showMainMenu()
                    return@Thread
                }
                walletHandle = handle
                Logger.log("CLI", "Wallet restored from seed")
                connectAndShowDashboard()
            } catch (e: Throwable) {
                Logger.error("CLI", "recoveryWallet failed", e)
                writeln("ERROR: ${e.message}")
                showMainMenu()
            }
        }.start()
    }

    // ── Restore from zip ───────────────────────────────────────────────────

    private fun promptZipPath() {
        writeln()
        writeln("Place the backup zip file in your Downloads folder,")
        writeln("then enter the filename (e.g. wallet-backup.zip):")
        write("> ")
        state = State.AWAIT_ZIP_PATH
    }

    private fun handleZipPath(input: String) {
        val zipFile = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            input.trim()
        )
        if (!zipFile.exists()) {
            writeln("\nFile not found: ${zipFile.absolutePath}")
            promptZipPath()
            return
        }
        writeln("\nExtracting backup...")
        Thread {
            try { extractWalletZip(zipFile) }
            catch (e: Throwable) {
                Logger.error("CLI", "extractZip failed", e)
                writeln("ERROR: ${e.message}")
                showMainMenu()
            }
        }.start()
    }

    private fun extractWalletZip(zipFile: File) {
        val zis = ZipInputStream(zipFile.inputStream())
        var entry = zis.nextEntry
        var foundWallet = false
        while (entry != null) {
            val name = File(entry.name).name
            if (name in listOf("wallet", "wallet.keys", ".shared-ringdb")) {
                File(walletDir, name).outputStream().use { zis.copyTo(it) }
                if (name == "wallet") foundWallet = true
            }
            zis.closeEntry()
            entry = zis.nextEntry
        }
        zis.close()
        if (!foundWallet) {
            writeln("ERROR: Zip does not contain a valid wallet file.")
            showMainMenu()
            return
        }
        writeln("Extraction successful.")
        writeln()
        write("Wallet password: ")
        state = State.AWAIT_PASSWORD
    }

    // ── Open Wallet ────────────────────────────────────────────────────────

    private fun promptPassword() {
        val name = File(defaultWalletPath).name
        writeln("Wallet found: $name")
        writeln()
        write("Password: ")
        state = State.AWAIT_PASSWORD
    }

    private fun handlePassword(input: String) {
        writeln("\nOpening wallet...")
        Thread {
            try {
                val handle = WalletJni.openWallet(defaultWalletPath, input)
                if (handle == 0L) {
                    writeln("ERROR: Wrong password or corrupted wallet.")
                    promptPassword()
                    return@Thread
                }
                walletHandle = handle
                Logger.log("CLI", "Wallet opened")
                connectAndShowDashboard()
            } catch (e: Throwable) {
                Logger.error("CLI", "openWallet failed", e)
                writeln("ERROR: ${e.message}")
                promptPassword()
            }
        }.start()
    }

    // ── Connect & Dashboard ────────────────────────────────────────────────

    private fun connectAndShowDashboard() {
        writeln("\nConnecting to node: $defaultNode ...")
        try {
            WalletJni.initWallet(walletHandle, defaultNode)
            Logger.log("CLI", "Connected to $defaultNode")
            writeln("Connected.")
        } catch (e: Throwable) {
            Logger.error("CLI", "initWallet failed", e)
            writeln("WARNING: Could not connect to node. Offline mode.")
        }
        showDashboard()
    }

    private fun showDashboard() {
        val address  = try { WalletJni.getAddress(walletHandle) } catch (e: Throwable) { "unavailable" }
        val balance  = try { WalletJni.getBalance(walletHandle) } catch (e: Throwable) { 0L }
        val unlocked = try { WalletJni.getUnlockedBalance(walletHandle) } catch (e: Throwable) { 0L }

        writeln()
        separator()
        writeln("  Address  : ${address.take(20)}...")
        writeln("  Balance  : %.12f XMR".format(WalletJni.atomicToXmr(balance)))
        writeln("  Unlocked : %.12f XMR".format(WalletJni.atomicToXmr(unlocked)))
        writeln("  Node     : $defaultNode")
        separator()
        writeln()
        writeln("  [1] Receive XMR")
        writeln("  [2] Send XMR")
        writeln("  [3] Transaction history")
        writeln("  [4] Backup wallet to Downloads")
        writeln("  [5] Show seed phrase")
        writeln("  [6] Lock / switch wallet")
        writeln()
        write("Select: ")
        state = State.DASHBOARD
    }

    private fun handleDashboard(input: String) {
        when (input) {
            "1"  -> showReceive()
            "2"  -> promptSendAddress()
            "3"  -> showHistory()
            "4"  -> backupWallet()
            "5"  -> showSeed()
            "6"  -> lockWallet()
            else -> { writeln("\nInvalid option."); showDashboard() }
        }
    }

    // ── Receive ────────────────────────────────────────────────────────────

    private fun showReceive() {
        val address = try { WalletJni.getAddress(walletHandle) } catch (e: Throwable) { "unavailable" }
        writeln()
        writeln("Your Monero address:")
        separator()
        writeln(address)
        separator()
        writeln()
        write("Press Enter to go back...")
        state = State.DASHBOARD
    }

    // ── Send ───────────────────────────────────────────────────────────────

    private fun promptSendAddress() {
        writeln()
        write("Recipient address: ")
        state = State.AWAIT_SEND_ADDRESS
    }

    private fun handleSendAddress(input: String) {
        sendAddress = input.trim()
        writeln()
        write("Amount (XMR): ")
        state = State.AWAIT_SEND_AMOUNT
    }

    private fun handleSendAmount(input: String) {
        val amount = input.trim().toDoubleOrNull()
        if (amount == null || amount <= 0) {
            writeln("\nInvalid amount.")
            write("Amount (XMR): ")
            return
        }
        writeln()
        writeln("Send XMR feature coming soon.")
        writeln("(Requires PendingTransaction implementation)")
        writeln()
        showDashboard()
    }

    // ── History ────────────────────────────────────────────────────────────

    private fun showHistory() {
        writeln()
        writeln("Transaction history coming soon.")
        writeln("(Requires node sync)")
        writeln()
        write("Press Enter to go back...")
        state = State.DASHBOARD
    }

    // ── Backup ─────────────────────────────────────────────────────────────

    private fun backupWallet() {
        writeln("\nCreating backup...")
        Thread {
            try {
                val zipName = "monclia-backup-${System.currentTimeMillis()}.zip"
                val zipFile = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    zipName
                )
                ZipOutputStream(zipFile.outputStream()).use { zos ->
                    for (name in listOf("wallet", "wallet.keys", ".shared-ringdb")) {
                        val f = File(walletDir, name)
                        if (!f.exists()) continue
                        zos.putNextEntry(ZipEntry(name))
                        f.inputStream().copyTo(zos)
                        zos.closeEntry()
                    }
                }
                Logger.log("CLI", "Backup saved: $zipName")
                writeln("Backup saved to Downloads: $zipName")
                writeln("Compatible with Monero GUI and CLI wallets.")
            } catch (e: Throwable) {
                Logger.error("CLI", "backup failed", e)
                writeln("ERROR: ${e.message}")
            }
            writeln()
            showDashboard()
        }.start()
    }

    // ── Seed ───────────────────────────────────────────────────────────────

    private fun showSeed() {
        val seed = try { WalletJni.getSeed(walletHandle) } catch (e: Throwable) { "unavailable" }
        writeln()
        writeln("SEED PHRASE — do not share with anyone:")
        separator()
        writeln(seed)
        separator()
        writeln()
        write("Press Enter to go back...")
        state = State.DASHBOARD
    }

    // ── Lock ───────────────────────────────────────────────────────────────

    private fun lockWallet() {
        WalletJni.closeWallet(walletHandle)
        walletHandle = 0L
        Logger.log("CLI", "Wallet locked")
        clear()
        header()
        showMainMenu()
    }
}
