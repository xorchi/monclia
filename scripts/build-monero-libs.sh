#!/usr/bin/env bash
set -euo pipefail

BUILD_DIR="$(pwd)/build/monero"
MONERO_SRC="$(pwd)/external/monero"
OPENSSL_DIR="$(pwd)/build/openssl"
BOOST_DIR="$(pwd)/build/boost"
SODIUM_DIR="$(pwd)/build/libsodium"
UNBOUND_DIR="$(pwd)/build/libunbound"
ZMQ_DIR="$(pwd)/build/libzmq"
API_LEVEL="${API_LEVEL:-24}"

mkdir -p "${BUILD_DIR}"
rm -rf "${BUILD_DIR}/CMakeCache.txt" "${BUILD_DIR}/CMakeFiles"

# With --layout=tagged, b2 produces names like libboost_filesystem-mt.a
# BoostConfig.cmake is at: ${BOOST_DIR}/lib/cmake/Boost-1.87.0/BoostConfig.cmake
# CMAKE_PREFIX_PATH must point to ${BOOST_DIR} so CMake finds lib/cmake/Boost-*/
echo "[monero] Boost lib contents:"
ls "${BOOST_DIR}/lib/" | grep -v cmake | head -15
echo "[monero] Boost cmake dirs:"
ls "${BOOST_DIR}/lib/cmake/" | head -10

echo "[monero] Configuring..."
cmake -S "${MONERO_SRC}" -B "${BUILD_DIR}" \
  -DCMAKE_TOOLCHAIN_FILE="${ANDROID_NDK_HOME}/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI="armeabi-v7a" \
  -DANDROID_PLATFORM="android-${API_LEVEL}" \
  -DCMAKE_BUILD_TYPE=Release \
  -DCMAKE_POLICY_DEFAULT_CMP0167=NEW \
  -DCMAKE_PREFIX_PATH="${BOOST_DIR};${OPENSSL_DIR};${SODIUM_DIR};${UNBOUND_DIR};${ZMQ_DIR}" \
  -DSTATIC=ON \
  -DBUILD_GUI_DEPS=ON \
  -DBUILD_TESTS=OFF \
  -DBUILD_DOCUMENTATION=OFF \
  -DUSE_DEVICE_TREZOR=OFF \
  -DCMAKE_FIND_ROOT_PATH="${BOOST_DIR};${OPENSSL_DIR};${SODIUM_DIR};${UNBOUND_DIR};${ZMQ_DIR}" \
  -DCMAKE_FIND_ROOT_PATH_MODE_PACKAGE=BOTH \
  -DCMAKE_FIND_ROOT_PATH_MODE_LIBRARY=BOTH \
  -DCMAKE_FIND_ROOT_PATH_MODE_INCLUDE=BOTH \
  -DOPENSSL_ROOT_DIR="${OPENSSL_DIR}" \
  -DOPENSSL_INCLUDE_DIR="${OPENSSL_DIR}/include" \
  -DOPENSSL_SSL_LIBRARY:FILEPATH="${OPENSSL_DIR}/lib/libssl.a" \
  -DOPENSSL_CRYPTO_LIBRARY:FILEPATH="${OPENSSL_DIR}/lib/libcrypto.a" \
  -DBOOST_ROOT="${BOOST_DIR}" \
  -DBoost_ROOT="${BOOST_DIR}" \
  -DBoost_USE_STATIC_LIBS=ON \
  -DBoost_USE_MULTITHREADED=ON \
  -DSodium_INCLUDE_DIR="${SODIUM_DIR}/include" \
  -DSodium_LIBRARY:FILEPATH="${SODIUM_DIR}/lib/libsodium.a" \
  -DUNBOUND_ROOT="${UNBOUND_DIR}" \
  -DUNBOUND_INCLUDE_DIR="${UNBOUND_DIR}/include" \
  -DUNBOUND_LIBRARIES:FILEPATH="${UNBOUND_DIR}/lib/libunbound.a" \
  -DZMQ_INCLUDE_DIR="${ZMQ_DIR}/include" \
  -DZMQ_LIBRARY:FILEPATH="${ZMQ_DIR}/lib/libzmq.a"

echo "[monero] Building wallet_api..."
cmake --build "${BUILD_DIR}" \
  --target wallet_api \
  -j"${JOBS:-4}"

echo "[monero] Done. Output: ${BUILD_DIR}"
