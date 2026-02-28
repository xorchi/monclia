#!/usr/bin/env bash
set -euo pipefail

# Config
OPENSSL_VERSION="3.4.1"
OPENSSL_URL="https://github.com/openssl/openssl/releases/download/openssl-${OPENSSL_VERSION}/openssl-${OPENSSL_VERSION}.tar.gz"
BUILD_DIR="$(pwd)/build/openssl"
SRC_DIR="/tmp/openssl-${OPENSSL_VERSION}"

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${API_LEVEL:-24}"

export CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
export CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
export AR="${TOOLCHAIN}/bin/llvm-ar"
export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
export STRIP="${TOOLCHAIN}/bin/llvm-strip"

mkdir -p "${BUILD_DIR}" "${SRC_DIR}"

echo "[openssl] Downloading..."
curl -L "${OPENSSL_URL}" | tar xz -C /tmp

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
