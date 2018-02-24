//
// Created by xuss on 2016/11/20.
//
#define LOG_TAG "BarCodeSerialUpdateNative"
#include <android/log.h>
#include <jni.h>
#include <string.h>
#include <pthread.h>
#include "com_inspiry_barcodeupdate_BarCodeSerialUpdate.h"
#include "SerialDevice.h"
#include "EventListener.h"
#include "InspiryLog.h"

typedef struct  {
    jfieldID mDeviceNativePointer;
	jfieldID mDeviceNativeCallBackPointer;
    jmethodID postEvent;
}fields_t;
static fields_t g_field;
static SerialDevice * g_serialDevice = NULL;
const char * BarCodeSerialUpdate_mDeviceNativePointer_Jni_Id = "mDeviceNativePointer";
const char * BarCodeSerialUpdate_mDeviceNativeCallBackPointer_Jni_Id = "mDeviceNativeCallBackPointer";

static JavaVM *gVM=NULL;
static JavaVM* getJavaVM()
{
    return gVM;
}
static void setJavaVM(JavaVM * vm)
{
    gVM = vm;
}
class BarCodeSerialUpdateEventListener :public EventListener
{
public:
    BarCodeSerialUpdateEventListener(JNIEnv* env, jobject weakThiz, jclass clazz);
    ~BarCodeSerialUpdateEventListener();
    virtual void onEvent(int what, int arg1, int arg2);
private:
    static JNIEnv* getJNIEnv(bool* needsDetach);
    static void detachJNI();
private:
    jobject mWeakThiz;
    jclass mClazz;
};
BarCodeSerialUpdateEventListener::BarCodeSerialUpdateEventListener(JNIEnv* env, jobject weakThiz, jclass clazz) :
        mWeakThiz(env->NewGlobalRef(weakThiz)),
        mClazz((jclass)env->NewGlobalRef(clazz))
{}
void BarCodeSerialUpdateEventListener::onEvent( int what, int arg1, int arg2)
{
    bool needsDetach = false;
    JNIEnv* env = getJNIEnv(&needsDetach);
    if (env != NULL) {
        LOGD(LOG_TAG,"onEvent event is posted");
        env->CallStaticVoidMethod(mClazz, g_field.postEvent,  what, arg1, arg2,mWeakThiz);
    } else {
        LOGE(LOG_TAG,"onEvent event will not posted");
    }
    if (needsDetach) {
        detachJNI();
    }
}
JNIEnv* BarCodeSerialUpdateEventListener::getJNIEnv(bool* needsDetach) {
    *needsDetach = false;
    JavaVM* vm = getJavaVM();
    JNIEnv*env = NULL;
    if(vm->GetEnv((void**) &env, JNI_VERSION_1_4) != JNI_OK)
    {
        env = NULL;
    }

    if (env == NULL) {
        JavaVMAttachArgs args = {JNI_VERSION_1_4, NULL, NULL};
        JavaVM* vm = getJavaVM();
        int result = vm->AttachCurrentThread(&env, (void*) &args);
        if (result != JNI_OK) {
            LOGE(LOG_TAG,"thread attach failed: %#x", result);
            return NULL;
        }
        *needsDetach = true;
    }

    if(env==NULL)
    {
        LOGE(LOG_TAG,"getJNIEnv env is NULL!!!");
    }
    return env;
}
void BarCodeSerialUpdateEventListener::detachJNI() {

    JavaVM* vm = getJavaVM();
    int result = vm->DetachCurrentThread();
    if (result != JNI_OK) {
        LOGE(LOG_TAG,"thread detach failed: %#x", result);
    }

}
BarCodeSerialUpdateEventListener::~BarCodeSerialUpdateEventListener()
{
    bool needsDetach = false;
    JNIEnv* env = getJNIEnv(&needsDetach);
    if (env != NULL) {
        env->DeleteGlobalRef(mWeakThiz);
        env->DeleteGlobalRef(mClazz);
    } else {
        LOGE(LOG_TAG,"leaking JNI object references");
    }
    if (needsDetach) {
        detachJNI();
    }
}
/*
 * nativeClassInit
 */
JNIEXPORT void JNICALL Java_com_inspiry_barcodeupdate_BarCodeSerialUpdate_nativeClassInit
  (JNIEnv *env, jclass classzz)
  {
    g_field.mDeviceNativePointer = env->GetFieldID(classzz, BarCodeSerialUpdate_mDeviceNativePointer_Jni_Id, "J");
    if(g_field.mDeviceNativePointer == NULL)
    {
      LOGE(LOG_TAG, "can't find com/inspiry/barcodeupdate/BarCodeSerialUpdate.%s",
              BarCodeSerialUpdate_mDeviceNativePointer_Jni_Id);
    }
	g_field.mDeviceNativeCallBackPointer = env->GetFieldID(classzz, BarCodeSerialUpdate_mDeviceNativeCallBackPointer_Jni_Id, "J");
    if(g_field.mDeviceNativePointer == NULL)
    {
      LOGE(LOG_TAG, "can't find com/inspiry/barcodeupdate/BarCodeSerialUpdate.%s",
              BarCodeSerialUpdate_mDeviceNativeCallBackPointer_Jni_Id);
    }
	g_field.postEvent = env->GetStaticMethodID(classzz, "postEventFromNative",
											"(IIILjava/lang/Object;)V");
	if (g_field.postEvent == NULL) {
	  LOGE(LOG_TAG,"can't find com/inspiry/barcodeupdate/BarCodeSerialUpdate.postEventFromNative");
	}
	env->GetJavaVM(&gVM); //保存到全局变量中JVM

  }
static void BarCodeSerialUpdate_setDeviceNativePointer(JNIEnv* env, jobject thizz, SerialDevice * pdevice)
{
    env->SetLongField(thizz, g_field.mDeviceNativePointer, (long)pdevice);
}
static void BarCodeSerialUpdate_setDeviceNativeCallBackPointer(JNIEnv* env, jobject thizz, BarCodeSerialUpdateEventListener * pEventListener)
{
    env->SetLongField(thizz, g_field.mDeviceNativeCallBackPointer, (long)pEventListener);
}
/**
 * openNative
 * return   -1 open error
 *          -2 device is opened
 */
JNIEXPORT jint JNICALL Java_com_inspiry_barcodeupdate_BarCodeSerialUpdate_openNative
        (JNIEnv *env, jobject thizz, jobject weakThiz, jstring path, jint baudrate, jint parity, jint stop, jint bits)
{

    SerialDevice * pdevice = NULL;
    const char * deviceName = NULL;
	BarCodeSerialUpdateEventListener *pEventListener = NULL;
    if(g_serialDevice != NULL)
    {
        LOGE(LOG_TAG, "openNative: pdevice is already opened!!!");
        return -2;
    }
    deviceName = path != NULL ? env->GetStringUTFChars(path, NULL):"/dev/ttyS0";
    pdevice = new SerialDevice(deviceName, baudrate, parity, stop, bits);
    if(pdevice == NULL)
    {
        LOGE(LOG_TAG, "openNative: pdevice is NULL!!!");
        return -1;
    }
    BarCodeSerialUpdate_setDeviceNativePointer(env, thizz, pdevice);

    jclass clazz = env->GetObjectClass(thizz);
	if (clazz == NULL) {
		LOGE(LOG_TAG, "openNative: env->GetObjectClass error!!!");
		return -1;
	}
	pEventListener = new BarCodeSerialUpdateEventListener(env, weakThiz, clazz);
	BarCodeSerialUpdate_setDeviceNativeCallBackPointer(env, thizz, pEventListener);
    pdevice->setEventListener(pEventListener);

    if(pdevice->openDevice() != 0)
    {
        LOGE(LOG_TAG, "openNative: open device is error!!!");
        goto ERROR;
    }
    g_serialDevice = pdevice;
    return 0;
ERROR:
        delete pdevice;
        g_serialDevice = NULL;
        return -1;

}
/*
 * closeNative
 */
JNIEXPORT jint JNICALL Java_com_inspiry_barcodeupdate_BarCodeSerialUpdate_closeNative
        (JNIEnv *env, jobject thizz)
{
    SerialDevice * pdevice = NULL;
	BarCodeSerialUpdateEventListener *pEventListener = NULL;
    pdevice = (SerialDevice*)env->GetLongField(thizz, g_field.mDeviceNativePointer);
	pEventListener = (BarCodeSerialUpdateEventListener*)env->GetLongField(thizz, g_field.mDeviceNativeCallBackPointer);
    if(pdevice == NULL)
    {
        LOGE(LOG_TAG, "closeNative: pdevice is NULL");
        return -1;
    }
	if(pEventListener == NULL)
	{
		LOGE(LOG_TAG, "closeNative: pEventListener is NULL");
		return -1;
	}
    if(pdevice->closeDevice() != 0)
    {
        LOGE(LOG_TAG, "closeNative: pdevice error");
        return -1;
    }
    delete  pdevice;
	delete 	pEventListener;
    LOGD(LOG_TAG, "closeNative: success");
    env->SetLongField(thizz, g_field.mDeviceNativePointer, 0);
	env->SetLongField(thizz, g_field.mDeviceNativeCallBackPointer, 0);
    g_serialDevice = NULL;
    return 0;
}
/*
 * updateNative
 */
JNIEXPORT jint JNICALL Java_com_inspiry_barcodeupdate_BarCodeSerialUpdate_updateNative
        (JNIEnv *env, jobject thizz, jstring path, jstring md5Path)
{
    int ret = 0;
    SerialDevice * pdevice = NULL;
    pdevice = (SerialDevice*)env->GetLongField(thizz, g_field.mDeviceNativePointer);

    if(pdevice == NULL)
    {
        LOGE(LOG_TAG, "closeNative: pdevice is NULL");
        return 1;//false
    }
    const char * packagePath= path!=NULL?env->GetStringUTFChars(path, NULL):NULL;
    const char * md5checkPath= md5Path!=NULL?env->GetStringUTFChars(md5Path, NULL):NULL;

    if(packagePath == NULL || md5checkPath == NULL)
    {
        LOGE(LOG_TAG, "updateNative: packagePath or md5checkPath is null!!!");
        return 1;//false
    }

    return pdevice->upgrade(packagePath, md5checkPath);
}
JNIEXPORT jstring JNICALL Java_com_inspiry_barcodeupdate_BarCodeSerialUpdate_getVersionNative
        (JNIEnv *env, jobject thizz)
{
    SerialDevice * pdevice = NULL;
    pdevice = (SerialDevice*)env->GetLongField(thizz, g_field.mDeviceNativePointer);

    if(pdevice == NULL)
    {
        LOGE(LOG_TAG, "closeNative: pdevice is NULL");
        return NULL;
    }
    const char * version = pdevice->getTargetVersion();
    if(version != NULL)
    {
        return env->NewStringUTF(version);
    }
    else
    {
		LOGE(LOG_TAG, "version is NULL");
        return NULL;
    }
}

JNIEXPORT jstring JNICALL Java_com_inspiry_barcodeupdate_BarCodeSerialUpdate_getTargetHardwareNameNative
  (JNIEnv *env, jobject thizz)
  {
	SerialDevice * pdevice = NULL;
	pdevice = (SerialDevice*)env->GetLongField(thizz, g_field.mDeviceNativePointer);
	if(pdevice == NULL)
    {
        LOGE(LOG_TAG, "closeNative: pdevice is NULL");
        return NULL;
    }
    const char * produceName = pdevice->getProduceName();
    if(produceName != NULL)
    {
        return env->NewStringUTF(produceName);
    }
    else
    {
		LOGE(LOG_TAG, "produceName is NULL");
        return NULL;
    }
  }
