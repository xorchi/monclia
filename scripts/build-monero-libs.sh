#!/usr/bin/env bash
set -euo pipefail

BUILD_DIR="$(pwd)/build/monero"
MONERO_SRC="$(pwd)/external/monero"
OPENSSL_DIR="$(pwd)/build/openssl"
BOOST_DIR="$(pwd)/build/boost"
SODIUM_DIR="$(pwd)/build/libsodium"
UNBOUND_DIR="$(pwd)/build/libunbound"
API_LEVEL="${API_LEVEL:-24}"

mkdir -p "${BUILD_DIR}"

echo "[monero] Configuring..."
cmake -S "${MONERO_SRC}" -B "${BUILD_DIR}" \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="armeabi-v7a" \
  -DANDROID_PLATFORM="android-${API_LEVEL}" \
  -DCMAKE_BUILD_TYPE=Release \
  -DSTATIC=ON \
  -DBUILD_GUI_DEPS=ON \
  -DBUILD_TESTS=OFF \
  -DBUILD_DOCUMENTATION=OFF \
  -DUSE_DEVICE_TREZOR=OFF \
  -DOPENSSL_ROOT_DIR="${OPENSSL_DIR}" \
  -DBOOST_ROOT="${BOOST_DIR}" \
  -DSodium_ROOT="${SODIUM_DIR}" \
  -DUNBOUND_ROOT="${UNBOUND_DIR}"

echo "[monero] Building wallet libraries..."
cmake --build "${BUILD_DIR}" \
  --target wallet_api \
  -j"${JOBS:-4}"

echo "[monero] Done. Output: ${BUILD_DIR}"
