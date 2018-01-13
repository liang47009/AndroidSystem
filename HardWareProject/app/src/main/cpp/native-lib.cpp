#include <jni.h>
#include <string>
#include <android/log.h>
#include <hardware/hardware.h>
#include <dlfcn.h>

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "MyApp", __VA_ARGS__)

extern "C" {
/*
 * Prototypes for functions exported by loadable shared libs.  These are
 * called by JNI, not provided by JNI.
 */
JNIEXPORT jint JNI_OnLoad(JavaVM *, void *) {
    LOGI("JNI_OnLoad");
    return JNI_VERSION_1_4;
}

JNIEXPORT void JNI_OnUnload(JavaVM *, void *) {
    LOGI("JNI_OnUnload");
}

JNIEXPORT jstring JNICALL
Java_com_mufeng_myapplication1_MainActivity_stringFromJNI(JNIEnv *env, jobject) {
    std::string hello = "Hello from C++";
    return env->NewStringUTF(hello.c_str());
}

JNIEXPORT jstring JNICALL
Java_com_mufeng_myapplication1_MainActivity_dlerror(JNIEnv *env, jobject) {
    char const *err_str = dlerror();
    return env->NewStringUTF(err_str);
}

JNIEXPORT void JNICALL
Java_com_mufeng_myapplication1_MainActivity_queryHardWare(JNIEnv *env, jobject, jstring libPath,
                                                          jstring libName) {
    hw_module_t const *module;
    const char *cLibPath = env->GetStringUTFChars(libPath, NULL);
    const char *cLibName = env->GetStringUTFChars(libName, NULL);
    if (hw_get_module(cLibName, &module) == 0) {
        LOGI("Opened %s Module", cLibName);
        LOGI("module->author: %s", module->author);
        LOGI("module->name: %s", module->name);
        LOGI("module->id: %s", module->id);
        LOGI("module->module_api_version: %d", module->module_api_version);
        LOGI("module->hal_api_version: %d", module->hal_api_version);
        LOGI("module->tag: %d", module->tag);
        struct hw_module_methods_t *methods = module->methods;
        struct hw_device_t *device;
        int ret = methods->open(module, module->id, &device);
        LOGI("methods->open: %d", ret);
        if (ret == 0) {
            ret = device->close(device);
            LOGI("methods->close: %d", ret);
        }
    } else {
        LOGI("%s HW Module not found", cLibName);
    }

    env->ReleaseStringUTFChars(libPath, cLibPath);
    env->ReleaseStringUTFChars(libName, cLibName);
}

}