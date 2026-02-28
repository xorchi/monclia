#!/usr/bin/env bash
set -euo pipefail

LIBSODIUM_VERSION="1.0.20"
LIBSODIUM_URL="https://github.com/jedisct1/libsodium/releases/download/${LIBSODIUM_VERSION}-RELEASE/libsodium-${LIBSODIUM_VERSION}.tar.gz"
BUILD_DIR="$(pwd)/build/libsodium"
SRC_DIR="/tmp/libsodium-${LIBSODIUM_VERSION}"

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${API_LEVEL:-24}"

export CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
export CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
export AR="${TOOLCHAIN}/bin/llvm-ar"
export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
export STRIP="${TOOLCHAIN}/bin/llvm-strip"

mkdir -p "${BUILD_DIR}"

echo "[libsodium] Downloading..."
curl -L "${LIBSODIUM_URL}" | tar xz -C /tmp

echo "[libsodium] Configuring..."
cd "${SRC_DIR}"
./configure \
  --host=arm-linux-androideabi \
  --prefix="${BUILD_DIR}" \
  --disable-shared \
  --enable-static \
  --with-pic

echo "[libsodium] Building..."
make -j"${JOBS:-4}"

echo "[libsodium] Installing..."
make install

echo "[libsodium] Done. Output: ${BUILD_DIR}"
