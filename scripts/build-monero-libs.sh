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

echo "[monero] Checking Boost structure..."
find "${BOOST_DIR}" -maxdepth 4 -name "uuid" -o -name "asio" | head -10
find "${BOOST_DIR}/include" -maxdepth 2 -type d | head -10 || echo "No include dir"

# Boost cmake install kadang taruh headers di include/boost-1_87/boost/
BOOST_INCLUDE="${BOOST_DIR}/include"
if [ -d "${BOOST_DIR}/include/boost-1_87" ]; then
  BOOST_INCLUDE="${BOOST_DIR}/include/boost-1_87"
fi
echo "Using BOOST_INCLUDE: ${BOOST_INCLUDE}"

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
  -DOPENSSL_INCLUDE_DIR="${OPENSSL_DIR}/include" \
  -DOPENSSL_SSL_LIBRARY="${OPENSSL_DIR}/lib/libssl.a" \
  -DOPENSSL_CRYPTO_LIBRARY="${OPENSSL_DIR}/lib/libcrypto.a" \
  -DBOOST_ROOT="${BOOST_DIR}" \
  -DBOOST_INCLUDEDIR="${BOOST_INCLUDE}" \
  -DBOOST_LIBRARYDIR="${BOOST_DIR}/lib" \
  -DBoost_NO_SYSTEM_PATHS=ON \
  -DCMAKE_CXX_FLAGS="-I${BOOST_INCLUDE}" \
  -DCMAKE_C_FLAGS="-I${BOOST_INCLUDE}" \
  -DSodium_INCLUDE_DIR="${SODIUM_DIR}/include" \
  -DSodium_LIBRARY="${SODIUM_DIR}/lib/libsodium.a" \
  -DUNBOUND_INCLUDE_DIR="${UNBOUND_DIR}/include" \
  -DUNBOUND_LIBRARY="${UNBOUND_DIR}/lib/libunbound.a"

echo "[monero] Building wallet libraries..."
cmake --build "${BUILD_DIR}" \
  --target wallet_api \
  -j"${JOBS:-4}"

echo "[monero] Done. Output: ${BUILD_DIR}"
