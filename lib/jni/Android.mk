
LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := inspiry_update
LOCAL_SRC_FILES := \
	com_inspiry_barcodeupdate_BarCodeSerialUpdate.cpp\
	SerialDevice.cpp\
	Uart.cpp\
	UartCmd.cpp\
	Com.cpp\
	EventListener.cpp\
	UpdateThread.cpp\
	Thread.cpp\
	InspiryLog.cpp\
	Mutex.cpp\
	Condition.cpp \
        md5.cpp \
	Timer.cpp
	
LOCAL_CFLAGS := -fpermissive
LOCAL_LDFLAGS := -Wl,--build-id
LOCAL_LDLIBS := -llog
include $(BUILD_SHARED_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := upgrade-android
LOCAL_SRC_FILES := \
	com_inspiry_barcodeupdate_BarCodeSerialUpdate.cpp\
	SerialDevice.cpp\
	Uart.cpp\
	UartCmd.cpp\
	Com.cpp\
	EventListener.cpp\
	UpdateThread.cpp\
	Thread.cpp\
	InspiryLog.cpp\
	Mutex.cpp\
	Condition.cpp\
	Timer.cpp\
        md5.cpp\
	Test.cpp
	
LOCAL_CFLAGS := -fpermissive  -pie -fPIE
LOCAL_LDFLAGS := -Wl,--build-id  -pie -fPIE
LOCAL_LDLIBS := -llog
include $(BUILD_EXECUTABLE)



