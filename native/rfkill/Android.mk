ifneq ($(TARGET_SIMULATOR),true)

LOCAL_PATH:= $(call my-dir)

include $(CLEAR_VARS)
LOCAL_SRC_FILES:= rfkill.c
LOCAL_MODULE := rfkill 

#LOCAL_SHARED_LIBRARIES := libcutils

include $(BUILD_EXECUTABLE)

LOCAL_MODULE_TAGS := optional

endif  # TARGET_SIMULATOR != true
