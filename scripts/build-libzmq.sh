#!/usr/bin/env bash
set -euo pipefail

LIBZMQ_VERSION="4.3.5"
LIBZMQ_URL="https://github.com/zeromq/libzmq/releases/download/v${LIBZMQ_VERSION}/zeromq-${LIBZMQ_VERSION}.tar.gz"
BUILD_DIR="$(pwd)/build/libzmq"
SRC_DIR="/tmp/zeromq-${LIBZMQ_VERSION}"

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${API_LEVEL:-24}"

mkdir -p "${BUILD_DIR}"

echo "[libzmq] Downloading..."
curl -L "${LIBZMQ_URL}" | tar xz -C /tmp

echo "[libzmq] Configuring..."
cmake -S "${SRC_DIR}" -B "${SRC_DIR}/build" \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="armeabi-v7a" \
  -DANDROID_PLATFORM="android-${API_LEVEL}" \
  -DCMAKE_INSTALL_PREFIX="${BUILD_DIR}" \
  -DBUILD_SHARED=OFF \
  -DBUILD_STATIC=ON \
  -DBUILD_TESTS=OFF \
  -DWITH_PERF_TOOL=OFF \
  -DZMQ_BUILD_TESTS=OFF \
  -DWITH_DOCS=OFF \
  -DWITH_TLS=OFF \
  -DWITH_LIBSODIUM=OFF

echo "[libzmq] Building..."
cmake --build "${SRC_DIR}/build" -j"${JOBS:-4}"

echo "[libzmq] Installing..."
cmake --install "${SRC_DIR}/build"

echo "[libzmq] Done. Output: ${BUILD_DIR}"
