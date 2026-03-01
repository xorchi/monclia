#include <jni.h>
#include <pty.h>
#include <unistd.h>
#include <fcntl.h>
#include <android/log.h>
#include <string>
#include <vector>

#define TAG "monclia-pty"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

extern "C" {

// Returns int array: [master_fd, pid] or [-1, -1] on error
JNIEXPORT jintArray JNICALL
Java_io_monclia_PtyBridge_spawnProcess(
    JNIEnv* env,
    jobject /* thiz */,
    jstring exec_path,
    jobjectArray args,
    jstring working_dir,
    jint rows,
    jint cols)
{
    jintArray result = env->NewIntArray(2);
    jint error_result[] = {-1, -1};

    int master_fd, slave_fd;
    struct winsize ws = {
        .ws_row = (unsigned short)rows,
        .ws_col = (unsigned short)cols,
    };

    if (openpty(&master_fd, &slave_fd, nullptr, nullptr, &ws) < 0) {
        LOGE("openpty failed: %s", strerror(errno));
        env->SetIntArrayRegion(result, 0, 2, error_result);
        return result;
    }

    // Build argv
    const char* exec = env->GetStringUTFChars(exec_path, nullptr);
    const char* wdir = env->GetStringUTFChars(working_dir, nullptr);

    int argc = env->GetArrayLength(args);
    std::vector<std::string> arg_strings;
    std::vector<const char*> argv;

    arg_strings.push_back(std::string(exec));
    argv.push_back(arg_strings[0].c_str());

    for (int i = 0; i < argc; i++) {
        jstring arg = (jstring)env->GetObjectArrayElement(args, i);
        const char* arg_str = env->GetStringUTFChars(arg, nullptr);
        arg_strings.push_back(std::string(arg_str));
        argv.push_back(arg_strings.back().c_str());
        env->ReleaseStringUTFChars(arg, arg_str);
    }
    argv.push_back(nullptr);

    pid_t pid = fork();
    if (pid < 0) {
        LOGE("fork failed: %s", strerror(errno));
        env->SetIntArrayRegion(result, 0, 2, error_result);
        env->ReleaseStringUTFChars(exec_path, exec);
        env->ReleaseStringUTFChars(working_dir, wdir);
        return result;
    }

    if (pid == 0) {
        // Child process
        close(master_fd);
        setsid();
        ioctl(slave_fd, TIOCSCTTY, 0);
        dup2(slave_fd, STDIN_FILENO);
        dup2(slave_fd, STDOUT_FILENO);
        dup2(slave_fd, STDERR_FILENO);
        if (slave_fd > STDERR_FILENO) close(slave_fd);

        chdir(wdir);

        setenv("TERM", "xterm-256color", 1);

        execv(exec, const_cast<char**>(argv.data()));
        LOGE("execv failed: %s", strerror(errno));
        _exit(1);
    }

    // Parent process
    close(slave_fd);
    env->ReleaseStringUTFChars(exec_path, exec);
    env->ReleaseStringUTFChars(working_dir, wdir);

    jint parent_result[] = {master_fd, pid};
    env->SetIntArrayRegion(result, 0, 2, parent_result);
    return result;
}

JNIEXPORT void JNICALL
Java_io_monclia_PtyBridge_resizePty(
    JNIEnv* /* env */,
    jobject /* thiz */,
    jint master_fd,
    jint rows,
    jint cols)
{
    struct winsize ws = {
        .ws_row = (unsigned short)rows,
        .ws_col = (unsigned short)cols,
    };
    ioctl(master_fd, TIOCSWINSZ, &ws);
}

} // extern "C"
