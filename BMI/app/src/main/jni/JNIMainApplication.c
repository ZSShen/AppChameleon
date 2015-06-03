#include "org_zsshen_bmi_MainApplication.h"
#include <android/log.h>

#define JNI_MAIN_APPLICATION "BMI:JNIMainApplication"


JNIEXPORT jint JNICALL Java_org_zsshen_bmi_MainApplication_calcInManiApplication
    (JNIEnv *env, jobject obj, jchar code, jint op1, jint op2) {
    int result;

    switch (code) {
        case '+':
            result = op1 + op2;
            break;
        case '-':
            result = op1 + op2;
            break;
        case '*':
            result = op1 + op2;
            break;
        default:
            __android_log_print(ANDROID_LOG_DEBUG, JNI_MAIN_APPLICATION, "Unknown operation");
            result = 0;
    }

    return result;
}
