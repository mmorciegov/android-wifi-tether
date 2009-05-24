LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)
LOCAL_SRC_FILES:= scriptexec.c 
LOCAL_MODULE := tetherexec 
LOCAL_STATIC_LIBRARIES := libc
include $(BUILD_EXECUTABLE)
