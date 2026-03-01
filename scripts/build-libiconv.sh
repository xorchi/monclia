#!/usr/bin/env bash
set -euo pipefail

LIBICONV_VERSION="1.17"
LIBICONV_URL="https://ftp.gnu.org/pub/gnu/libiconv/libiconv-${LIBICONV_VERSION}.tar.gz"
BUILD_DIR="$(pwd)/build/libiconv"
SRC_DIR="/tmp/libiconv-${LIBICONV_VERSION}"

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${API_LEVEL:-24}"

export CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
export CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
export AR="${TOOLCHAIN}/bin/llvm-ar"
export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
export STRIP="${TOOLCHAIN}/bin/llvm-strip"

mkdir -p "${BUILD_DIR}"

echo "[libiconv] Downloading..."
curl -L --fail --retry 3 --retry-delay 5 "${LIBICONV_URL}" | tar xz -C /tmp

echo "[libiconv] Configuring..."
cd "${SRC_DIR}"
./configure \
  --host=arm-linux-androideabi \
  --prefix="${BUILD_DIR}" \
  --disable-shared \
  --enable-static \
  --with-pic \
  --disable-nls

echo "[libiconv] Building..."
make -j"${JOBS:-4}"

echo "[libiconv] Installing..."
make install

echo "[libiconv] Done. Output: ${BUILD_DIR}"
