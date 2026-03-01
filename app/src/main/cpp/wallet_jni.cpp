#include <jni.h>
#include <string>
#include <android/log.h>
#include "include/wallet2_api.h"

#define TAG "MoncliaNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

using namespace Monero;

// Global wallet manager instance
static WalletManager* g_manager = nullptr;
static Wallet* g_wallet = nullptr;

static WalletManager* getManager() {
    if (!g_manager) {
        g_manager = WalletManagerFactory::getWalletManager();
    }
    return g_manager;
}

extern "C" {

// ── WalletManager ──────────────────────────────────────────────────────────

JNIEXPORT jboolean JNICALL
Java_io_monclia_WalletJni_walletExists(JNIEnv* env, jobject, jstring path) {
    const char* p = env->GetStringUTFChars(path, nullptr);
    bool exists = getManager()->walletExists(p);
    env->ReleaseStringUTFChars(path, p);
    return exists;
}

JNIEXPORT jlong JNICALL
Java_io_monclia_WalletJni_createWallet(JNIEnv* env, jobject,
    jstring path, jstring password, jstring language)
{
    const char* p = env->GetStringUTFChars(path, nullptr);
    const char* pw = env->GetStringUTFChars(password, nullptr);
    const char* lang = env->GetStringUTFChars(language, nullptr);

    Wallet* w = getManager()->createWallet(p, pw, lang, MAINNET);

    env->ReleaseStringUTFChars(path, p);
    env->ReleaseStringUTFChars(password, pw);
    env->ReleaseStringUTFChars(language, lang);

    if (w->status() != Wallet::Status_Ok) {
        LOGE("createWallet failed: %s", w->errorString().c_str());
        return 0;
    }
    g_wallet = w;
    return reinterpret_cast<jlong>(w);
}

JNIEXPORT jlong JNICALL
Java_io_monclia_WalletJni_openWallet(JNIEnv* env, jobject,
    jstring path, jstring password)
{
    const char* p = env->GetStringUTFChars(path, nullptr);
    const char* pw = env->GetStringUTFChars(password, nullptr);

    Wallet* w = getManager()->openWallet(p, pw, MAINNET);

    env->ReleaseStringUTFChars(path, p);
    env->ReleaseStringUTFChars(password, pw);

    if (w->status() != Wallet::Status_Ok) {
        LOGE("openWallet failed: %s", w->errorString().c_str());
        return 0;
    }
    g_wallet = w;
    return reinterpret_cast<jlong>(w);
}

JNIEXPORT jlong JNICALL
Java_io_monclia_WalletJni_recoveryWallet(JNIEnv* env, jobject,
    jstring path, jstring password, jstring mnemonic, jlong restoreHeight)
{
    const char* p = env->GetStringUTFChars(path, nullptr);
    const char* pw = env->GetStringUTFChars(password, nullptr);
    const char* mn = env->GetStringUTFChars(mnemonic, nullptr);

    Wallet* w = getManager()->recoveryWallet(p, pw, mn, MAINNET,
        static_cast<uint64_t>(restoreHeight));

    env->ReleaseStringUTFChars(path, p);
    env->ReleaseStringUTFChars(password, pw);
    env->ReleaseStringUTFChars(mnemonic, mn);

    if (w->status() != Wallet::Status_Ok) {
        LOGE("recoveryWallet failed: %s", w->errorString().c_str());
        return 0;
    }
    g_wallet = w;
    return reinterpret_cast<jlong>(w);
}

// ── Wallet operations ──────────────────────────────────────────────────────

JNIEXPORT jboolean JNICALL
Java_io_monclia_WalletJni_initWallet(JNIEnv* env, jobject,
    jlong handle, jstring daemonAddress)
{
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    const char* addr = env->GetStringUTFChars(daemonAddress, nullptr);
    bool ok = w->init(addr);
    env->ReleaseStringUTFChars(daemonAddress, addr);
    return ok;
}

JNIEXPORT jstring JNICALL
Java_io_monclia_WalletJni_getAddress(JNIEnv* env, jobject, jlong handle) {
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    return env->NewStringUTF(w->address(0, 0).c_str());
}

JNIEXPORT jstring JNICALL
Java_io_monclia_WalletJni_getSeed(JNIEnv* env, jobject, jlong handle) {
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    return env->NewStringUTF(w->seed().c_str());
}

JNIEXPORT jlong JNICALL
Java_io_monclia_WalletJni_getBalance(JNIEnv* env, jobject, jlong handle) {
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    return static_cast<jlong>(w->balance(0));
}

JNIEXPORT jlong JNICALL
Java_io_monclia_WalletJni_getUnlockedBalance(JNIEnv* env, jobject, jlong handle) {
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    return static_cast<jlong>(w->unlockedBalance(0));
}

JNIEXPORT jstring JNICALL
Java_io_monclia_WalletJni_getErrorString(JNIEnv* env, jobject, jlong handle) {
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    return env->NewStringUTF(w->errorString().c_str());
}

JNIEXPORT jint JNICALL
Java_io_monclia_WalletJni_getStatus(JNIEnv* env, jobject, jlong handle) {
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    return static_cast<jint>(w->status());
}

JNIEXPORT void JNICALL
Java_io_monclia_WalletJni_closeWallet(JNIEnv* env, jobject, jlong handle) {
    Wallet* w = reinterpret_cast<Wallet*>(handle);
    getManager()->closeWallet(w, true);
    if (g_wallet == w) g_wallet = nullptr;
}

} // extern "C"
