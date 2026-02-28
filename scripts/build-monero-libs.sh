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

echo "[monero] Checking dependency paths..."
echo "=== OpenSSL ==="
ls "${OPENSSL_DIR}/lib/" || echo "MISSING"
echo "=== Boost ==="
ls "${BOOST_DIR}/lib/" | head -5 || echo "MISSING"
echo "=== libsodium ==="
ls "${SODIUM_DIR}/lib/" || echo "MISSING"
echo "=== libunbound ==="
find "${UNBOUND_DIR}" -name "*.a" -o -name "*.h" | head -20 || echo "MISSING"

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
  -DZMQ_FOUND=OFF \
  -DWITH_ZMQ=OFF \
  -DOPENSSL_ROOT_DIR="${OPENSSL_DIR}" \
  -DOPENSSL_INCLUDE_DIR="${OPENSSL_DIR}/include" \
  -DOPENSSL_SSL_LIBRARY="${OPENSSL_DIR}/lib/libssl.a" \
  -DOPENSSL_CRYPTO_LIBRARY="${OPENSSL_DIR}/lib/libcrypto.a" \
  -DBOOST_ROOT="${BOOST_DIR}" \
  -DBOOST_INCLUDEDIR="${BOOST_DIR}/include" \
  -DBOOST_LIBRARYDIR="${BOOST_DIR}/lib" \
  -DBoost_NO_SYSTEM_PATHS=ON \
  -DBoost_INCLUDE_DIR="${BOOST_DIR}/include" \
  -DBoost_LIBRARY_DIR_RELEASE="${BOOST_DIR}/lib" \
  -DBoost_NO_BOOST_CMAKE=ON \
  -DSodium_INCLUDE_DIR="${SODIUM_DIR}/include" \
  -DSodium_LIBRARY="${SODIUM_DIR}/lib/libsodium.a" \
  -DUNBOUND_ROOT="${UNBOUND_DIR}" \
  -DUNBOUND_INCLUDE_DIR="${UNBOUND_DIR}/include" \
  -DUNBOUND_LIBRARIES="${UNBOUND_DIR}/lib/libunbound.a"

echo "[monero] Building wallet libraries..."
cmake --build "${BUILD_DIR}" \
  --target wallet_api \
  -j"${JOBS:-4}"

echo "[monero] Done. Output: ${BUILD_DIR}"
