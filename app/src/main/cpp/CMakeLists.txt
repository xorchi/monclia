cmake_minimum_required(VERSION 3.22.1)
project(monclia)

set(NATIVE_LIBS_DIR ${CMAKE_SOURCE_DIR}/../../../../../build)
set(MONERO_DIR ${NATIVE_LIBS_DIR}/monero)
set(BOOST_DIR ${NATIVE_LIBS_DIR}/boost)
set(OPENSSL_DIR ${NATIVE_LIBS_DIR}/openssl)
set(SODIUM_DIR ${NATIVE_LIBS_DIR}/libsodium)
set(UNBOUND_DIR ${NATIVE_LIBS_DIR}/libunbound)

# ── Monero ─────────────────────────────────────────────────────────────────
add_library(wallet_api STATIC IMPORTED)
set_target_properties(wallet_api PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/lib/libwallet_api.a)

add_library(wallet STATIC IMPORTED)
set_target_properties(wallet PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/lib/libwallet.a)

add_library(epee STATIC IMPORTED)
set_target_properties(epee PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/contrib/epee/src/libepee.a)

add_library(lmdb STATIC IMPORTED)
set_target_properties(lmdb PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/external/db_drivers/liblmdb/liblmdb.a)

add_library(easylogging STATIC IMPORTED)
set_target_properties(easylogging PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/external/easylogging++/libeasylogging.a)

add_library(randomx STATIC IMPORTED)
set_target_properties(randomx PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/external/randomx/librandomx.a)

add_library(blockchain_db STATIC IMPORTED)
set_target_properties(blockchain_db PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/blockchain_db/libblockchain_db.a)

add_library(blocks STATIC IMPORTED)
set_target_properties(blocks PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/blocks/libblocks.a)

add_library(checkpoints STATIC IMPORTED)
set_target_properties(checkpoints PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/checkpoints/libcheckpoints.a)

add_library(common STATIC IMPORTED)
set_target_properties(common PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/common/libcommon.a)

add_library(cncrypto STATIC IMPORTED)
set_target_properties(cncrypto PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/crypto/libcncrypto.a)

add_library(cryptonote_basic STATIC IMPORTED)
set_target_properties(cryptonote_basic PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/cryptonote_basic/libcryptonote_basic.a)

add_library(cryptonote_format_utils_basic STATIC IMPORTED)
set_target_properties(cryptonote_format_utils_basic PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/cryptonote_basic/libcryptonote_format_utils_basic.a)

add_library(cryptonote_core STATIC IMPORTED)
set_target_properties(cryptonote_core PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/cryptonote_core/libcryptonote_core.a)

add_library(device STATIC IMPORTED)
set_target_properties(device PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/device/libdevice.a)

add_library(device_trezor STATIC IMPORTED)
set_target_properties(device_trezor PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/device_trezor/libdevice_trezor.a)

add_library(hardforks STATIC IMPORTED)
set_target_properties(hardforks PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/hardforks/libhardforks.a)

add_library(version STATIC IMPORTED)
set_target_properties(version PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/libversion.a)

add_library(mnemonics STATIC IMPORTED)
set_target_properties(mnemonics PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/mnemonics/libmnemonics.a)

add_library(multisig STATIC IMPORTED)
set_target_properties(multisig PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/multisig/libmultisig.a)

add_library(net STATIC IMPORTED)
set_target_properties(net PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/net/libnet.a)

add_library(ringct STATIC IMPORTED)
set_target_properties(ringct PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/ringct/libringct.a)

add_library(ringct_basic STATIC IMPORTED)
set_target_properties(ringct_basic PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/ringct/libringct_basic.a)

add_library(rpc_base STATIC IMPORTED)
set_target_properties(rpc_base PROPERTIES IMPORTED_LOCATION ${MONERO_DIR}/src/rpc/librpc_base.a)

# ── Boost ──────────────────────────────────────────────────────────────────
add_library(boost_atomic STATIC IMPORTED)
set_target_properties(boost_atomic PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_atomic-mt-s-a32.a)

add_library(boost_chrono STATIC IMPORTED)
set_target_properties(boost_chrono PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_chrono-mt-s-a32.a)

add_library(boost_date_time STATIC IMPORTED)
set_target_properties(boost_date_time PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_date_time-mt-s-a32.a)

add_library(boost_filesystem STATIC IMPORTED)
set_target_properties(boost_filesystem PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_filesystem-mt-s-a32.a)

add_library(boost_locale STATIC IMPORTED)
set_target_properties(boost_locale PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_locale-mt-s-a32.a)

add_library(boost_program_options STATIC IMPORTED)
set_target_properties(boost_program_options PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_program_options-mt-s-a32.a)

add_library(boost_regex STATIC IMPORTED)
set_target_properties(boost_regex PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_regex-mt-s-a32.a)

add_library(boost_serialization STATIC IMPORTED)
set_target_properties(boost_serialization PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_serialization-mt-s-a32.a)

add_library(boost_system STATIC IMPORTED)
set_target_properties(boost_system PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_system-mt-s-a32.a)

add_library(boost_thread STATIC IMPORTED)
set_target_properties(boost_thread PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_thread-mt-s-a32.a)

add_library(boost_wserialization STATIC IMPORTED)
set_target_properties(boost_wserialization PROPERTIES IMPORTED_LOCATION ${BOOST_DIR}/lib/libboost_wserialization-mt-s-a32.a)

# ── OpenSSL ────────────────────────────────────────────────────────────────
add_library(ssl STATIC IMPORTED)
set_target_properties(ssl PROPERTIES IMPORTED_LOCATION ${OPENSSL_DIR}/lib/libssl.a)

add_library(crypto STATIC IMPORTED)
set_target_properties(crypto PROPERTIES IMPORTED_LOCATION ${OPENSSL_DIR}/lib/libcrypto.a)

# ── libsodium ──────────────────────────────────────────────────────────────
add_library(sodium STATIC IMPORTED)
set_target_properties(sodium PROPERTIES IMPORTED_LOCATION ${SODIUM_DIR}/lib/libsodium.a)

# ── libunbound ─────────────────────────────────────────────────────────────
add_library(unbound STATIC IMPORTED)
set_target_properties(unbound PROPERTIES IMPORTED_LOCATION ${UNBOUND_DIR}/lib/libunbound.a)

# ── Main JNI shared library ────────────────────────────────────────────────
add_library(monclia-core SHARED wallet_jni.cpp pty_bridge.cpp)

target_include_directories(monclia-core PRIVATE
    include/
    ${NATIVE_LIBS_DIR}/../external/monero/src
    ${NATIVE_LIBS_DIR}/../external/monero/contrib/epee/include
    ${NATIVE_LIBS_DIR}/../external/monero/external
    ${BOOST_DIR}/include
    ${OPENSSL_DIR}/include
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(monclia-core
    wallet_api wallet epee lmdb easylogging randomx
    blockchain_db blocks checkpoints common cncrypto
    cryptonote_basic cryptonote_format_utils_basic cryptonote_core
    device device_trezor hardforks version mnemonics multisig
    net ringct ringct_basic rpc_base
    boost_atomic boost_chrono boost_date_time boost_filesystem
    boost_locale boost_program_options boost_regex boost_serialization
    boost_system boost_thread boost_wserialization
    ssl crypto sodium unbound
    ${log-lib} ${android-lib}
)
