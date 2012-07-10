LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE    := libwtnativetask
LOCAL_SRC_FILES := com_googlecode_android_wifi_tether_system_NativeTask.c 
#LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_SHARED_LIBRARY)
