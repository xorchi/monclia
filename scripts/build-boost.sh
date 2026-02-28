#!/usr/bin/env bash
set -euo pipefail

BOOST_VERSION="1.87.0"
BOOST_VERSION_UNDERSCORE="${BOOST_VERSION//./_}"
BOOST_URL="https://github.com/boostorg/boost/releases/download/boost-${BOOST_VERSION}/boost-${BOOST_VERSION}-cmake.tar.gz"
BUILD_DIR="$(pwd)/build/boost"
SRC_DIR="/tmp/boost-${BOOST_VERSION}"

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${API_LEVEL:-24}"

export CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
export CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
export AR="${TOOLCHAIN}/bin/llvm-ar"
export RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"

mkdir -p "${BUILD_DIR}" /tmp

echo "[boost] Downloading..."
curl -L "${BOOST_URL}" | tar xz -C /tmp

echo "[boost] Configuring..."
cd "${SRC_DIR}"
cmake -S . -B build \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="armeabi-v7a" \
  -DANDROID_PLATFORM="android-${API_LEVEL}" \
  -DCMAKE_INSTALL_PREFIX="${BUILD_DIR}" \
  -DBUILD_SHARED_LIBS=OFF \
  -DBOOST_ENABLE_CMAKE=ON \
  -DBOOST_INCLUDE_LIBRARIES="atomic;chrono;date_time;filesystem;program_options;regex;serialization;system;thread"

echo "[boost] Building..."
cmake --build build -j"${JOBS:-4}"

echo "[boost] Installing..."
cmake --install build

echo "[boost] Done. Output: ${BUILD_DIR}"
