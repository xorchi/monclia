package io.monclia

object WalletJni {

    init {
        System.loadLibrary("monclia-core")
    }

    // WalletManager
    external fun walletExists(path: String): Boolean
    external fun createWallet(path: String, password: String, language: String): Long
    external fun openWallet(path: String, password: String): Long
    external fun recoveryWallet(path: String, password: String, mnemonic: String, restoreHeight: Long): Long

    // Wallet operations
    external fun initWallet(handle: Long, daemonAddress: String): Boolean
    external fun getAddress(handle: Long): String
    external fun getSeed(handle: Long): String
    external fun getBalance(handle: Long): Long
    external fun getUnlockedBalance(handle: Long): Long
    external fun getStatus(handle: Long): Int
    external fun getErrorString(handle: Long): String
    external fun closeWallet(handle: Long)

    // Constants
    const val STATUS_OK = 0
    const val STATUS_ERROR = 1

    // Convert atomic units to XMR (1 XMR = 1e12 atomic units)
    fun atomicToXmr(atomic: Long): Double = atomic / 1_000_000_000_000.0
}
