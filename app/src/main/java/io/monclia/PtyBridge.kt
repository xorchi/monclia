package io.monclia

object PtyBridge {

    init {
        System.loadLibrary("monclia-core")
    }

    external fun spawnProcess(
        execPath: String,
        args: Array<String>,
        workingDir: String,
        rows: Int,
        cols: Int
    ): IntArray

    external fun resizePty(masterFd: Int, rows: Int, cols: Int)
}
