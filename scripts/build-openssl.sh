#!/usr/bin/env bash
set -euo pipefail

OPENSSL_VERSION="3.4.1"
OPENSSL_URL="https://github.com/openssl/openssl/releases/download/openssl-${OPENSSL_VERSION}/openssl-${OPENSSL_VERSION}.tar.gz"
BUILD_DIR="$(pwd)/build/openssl"
SRC_DIR="/tmp/openssl-${OPENSSL_VERSION}"
API_LEVEL="${API_LEVEL:-24}"

# OpenSSL modern menggunakan ANDROID_NDK_ROOT, bukan toolchain manual
export ANDROID_NDK_ROOT="${ANDROID_NDK_HOME}"
export PATH="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64/bin:${PATH}"

mkdir -p "${BUILD_DIR}"

echo "[openssl] Downloading..."
curl -L --fail --retry 3 --retry-delay 5 "${OPENSSL_URL}" | tar xz -C /tmp

echo "[openssl] Configuring..."
cd "${SRC_DIR}"

./Configure android-arm \
  -D__ANDROID_API__=${API_LEVEL} \
  --prefix="${BUILD_DIR}" \
  no-shared \
  no-tests \
  no-ui-console \
  no-unit-test

echo "[openssl] Building..."
make -j"${JOBS:-4}"

echo "[openssl] Installing..."
make install_sw

echo "[openssl] Done. Output: ${BUILD_DIR}"
