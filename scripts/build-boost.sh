#!/usr/bin/env bash
set -euo pipefail

BOOST_VERSION="1.87.0"
BOOST_VERSION_UNDERSCORE="${BOOST_VERSION//./_}"
BOOST_URL="https://archives.boost.io/release/${BOOST_VERSION}/source/boost_${BOOST_VERSION_UNDERSCORE}.tar.gz"
BUILD_DIR="$(pwd)/build/boost"
ICONV_DIR="$(pwd)/build/libiconv"
SRC_DIR="/tmp/boost_${BOOST_VERSION_UNDERSCORE}"

TOOLCHAIN="${ANDROID_NDK_HOME}/toolchains/llvm/prebuilt/linux-x86_64"
API_LEVEL="${API_LEVEL:-24}"

CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang"
CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API_LEVEL}-clang++"
AR="${TOOLCHAIN}/bin/llvm-ar"
RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"

mkdir -p "${BUILD_DIR}"

echo "[boost] Downloading source tarball..."
curl -L --fail --retry 3 --retry-delay 5 "${BOOST_URL}" | tar xz -C /tmp

echo "[boost] Bootstrapping..."
cd "${SRC_DIR}"
./bootstrap.sh --prefix="${BUILD_DIR}" \
  --with-libraries=atomic,chrono,date_time,filesystem,headers,locale,program_options,regex,serialization,system,thread

echo "[boost] Writing user-config.jam..."
cat > user-config.jam << JAMEOF
using clang : android :
    ${CXX}
    :
    <archiver>${AR}
    <ranlib>${RANLIB}
    <compileflags>"-target armv7a-linux-androideabi${API_LEVEL} --sysroot=${TOOLCHAIN}/sysroot -fPIC"
    <linkflags>"-target armv7a-linux-androideabi${API_LEVEL}"
    ;
JAMEOF

echo "[boost] Building..."
./b2 \
  toolset=clang-android \
  target-os=android \
  architecture=arm \
  address-model=32 \
  variant=release \
  link=static \
  threading=multi \
  runtime-link=static \
  --user-config=user-config.jam \
  --prefix="${BUILD_DIR}" \
  --layout=tagged \
  --with-headers \
  --with-atomic \
  --with-chrono \
  --with-date_time \
  --with-filesystem \
  --with-locale \
  boost.locale.iconv=on \
  boost.locale.icu=off \
  boost.locale.std=off \
  boost.locale.posix=off \
  -sICONV_PATH="${ICONV_DIR}" \
  --with-program_options \
  --with-regex \
  --with-serialization \
  --with-system \
  --with-thread \
  -j"${JOBS:-4}" \
  install

echo "[boost] Installed libs:"
ls "${BUILD_DIR}/lib/" | grep -v cmake | head -20
echo "[boost] Done. Output: ${BUILD_DIR}"
