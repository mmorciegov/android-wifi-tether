LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_PRELINK_MODULE:=false

LOCAL_SRC_FILES := android_tether_system_NativeTask.c 

LOCAL_SHARED_LIBRARIES := libcutils

LOCAL_C_INCLUDES += $(JNI_H_INCLUDE)

LOCAL_MODULE := libnativetask

include $(BUILD_SHARED_LIBRARY)
