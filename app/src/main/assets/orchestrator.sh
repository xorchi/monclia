#!/system/bin/sh
# orchestrator.sh - Monclia wallet orchestrator

WALLET_CLI="$BIN_DIR/monero-wallet-cli"
WALLET_FILE="$WALLET_DIR/main-wallet"
DAEMON="node.sethforprivacy.com:18089"
LOG_FILE="$LOG_DIR/monero-wallet-cli.log"

mkdir -p "$BIN_DIR" "$WALLET_DIR" "$LOG_DIR"

# ── Check binary ───────────────────────────────────────────────────────────
if [ ! -f "$WALLET_CLI" ]; then
    echo "ERROR: monero-wallet-cli not found."
    echo "Please restart the app to download it."
    exit 1
fi

# ── Build flags ────────────────────────────────────────────────────────────
FLAGS="--log-file $LOG_FILE --log-level 0 --daemon-address $DAEMON --trusted-daemon"

if [ -f "$WALLET_FILE" ]; then
    FLAGS="$FLAGS --wallet-file $WALLET_FILE"
fi

# ── Launch ─────────────────────────────────────────────────────────────────
exec "$WALLET_CLI" $FLAGS
