#!/usr/bin/env bash
set -euo pipefail

LIBUNBOUND_VERSION="1.22.0"
LIBUNBOUND_URL="https://nlnetlabs.nl/downloads/unbound/unbound-${LIBUNBOUND_VERSION}.tar.gz"
BUILD_DIR="$(pwd)/build/libunbound"
OPENSSL_DIR="$(pwd)/build/openssl"
SRC_DIR="/tmp/unbound-${LIBUNBOUND_VERSION}"

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${API_LEVEL:-24}"

export CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
export CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
export AR="${TOOLCHAIN}/bin/llvm-ar"
export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
export STRIP="${TOOLCHAIN}/bin/llvm-strip"

mkdir -p "${BUILD_DIR}"

echo "[libunbound] Downloading..."
curl -L "${LIBUNBOUND_URL}" | tar xz -C /tmp

echo "[libunbound] Configuring..."
cd "${SRC_DIR}"
./configure \
  --host=arm-linux-androideabi \
  --prefix="${BUILD_DIR}" \
  --disable-shared \
  --enable-static \
  --with-ssl="${OPENSSL_DIR}" \
  --with-pic \
  --disable-gost \
  --disable-ecdsa \
  --disable-dsa \
  --without-libexpat \
  --disable-unbound-event-api \
  --disable-dnstap \
  --disable-systemd \
  --disable-flto

echo "[libunbound] Building library only..."
make -j"${JOBS:-4}" lib

echo "[libunbound] Installing..."
make install-lib

echo "[libunbound] Done. Output: ${BUILD_DIR}"
