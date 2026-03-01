#!/system/bin/sh
# orchestrator.sh - Monclia wallet orchestrator
# Runs inside TerminalSession. User only sees monero-wallet-cli output.

set -e

WALLET_CLI="$BIN_DIR/monero-wallet-cli"
WALLET_FILE="$WALLET_DIR/main-wallet"
DAEMON="opennode.xmr-tw.org:18089"
LOG_FILE="$LOG_DIR/monero-wallet-cli.log"
DOWNLOAD_URL="https://downloads.getmonero.org/cli/monero-android-armv7-v0.18.4.5.tar.bz2"
EXPECTED_SHA256="3cd6611c5c33ae4c10e52698826560bbb17e00cf2f8a2d7f61e79d28f0f36ef6"

mkdir -p "$BIN_DIR" "$WALLET_DIR" "$LOG_DIR"

# ── Download monero-wallet-cli if not present ──────────────────────────────
if [ ! -f "$WALLET_CLI" ]; then
    echo "Downloading monero-wallet-cli..."
    ARCHIVE="$BIN_DIR/monero-android-armv7.tar.bz2"

    if ! curl -L --fail --retry 3 --retry-delay 5         --progress-bar         -o "$ARCHIVE"         "$DOWNLOAD_URL"; then
        echo "ERROR: Download failed. Check your internet connection."
        exit 1
    fi

    echo "Verifying integrity..."
    ACTUAL_SHA256=$(sha256sum "$ARCHIVE" | awk '{print $1}')
    if [ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" ]; then
        echo "ERROR: SHA256 mismatch. File may be corrupted."
        echo "Expected: $EXPECTED_SHA256"
        echo "Actual:   $ACTUAL_SHA256"
        rm -f "$ARCHIVE"
        exit 1
    fi

    echo "Extracting..."
    cd "$BIN_DIR"
    tar -xjf "$ARCHIVE" --wildcards "*/monero-wallet-cli" --strip-components=1
    rm -f "$ARCHIVE"
    chmod +x "$WALLET_CLI"
    echo "monero-wallet-cli ready."
    echo ""
fi

# ── Build flags ────────────────────────────────────────────────────────────
FLAGS="--log-file $LOG_FILE --log-level 0 --daemon-address $DAEMON --trusted-daemon"

if [ -f "$WALLET_FILE" ]; then
    FLAGS="$FLAGS --wallet-file $WALLET_FILE"
fi

# ── Launch ─────────────────────────────────────────────────────────────────
exec "$WALLET_CLI" $FLAGS
