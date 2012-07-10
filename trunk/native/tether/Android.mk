# Copyright 2009 The Android Open Source Project

LOCAL_PATH := $(call my-dir)

updater_src_files := \
        sha1.c\
	install.c\
	tether.c\

include $(CLEAR_VARS)

LOCAL_SRC_FILES := $(updater_src_files)

LOCAL_C_INCLUDES += $(dir $(inc))

LOCAL_CFLAGS := -DINTERNAL_SHA1 -DCONFIG_CRYPTO_INTERNAL -DCONFIG_NO_T_PRF -DCONFIG_NO_TLS_PRF

LOCAL_STATIC_LIBRARIES := libedify

LOCAL_SHARED_LIBRARIES := libcutils\
			  libhardware_legacy\
			  libc\
			  libnetutils\
			  libsysutils

LOCAL_MODULE := tether

include $(BUILD_EXECUTABLE)

#####################
#include $(CLEAR_VARS)
#LOCAL_SRC_FILES := ultra_bcm_config.c\
#                   wpa_supplicant/sha1.c\
#                   wpa_supplicant/md5.c\
#                   wpa_supplicant/os_unix.c\
#                   wpa_supplicant/des.c\
#                   wpa_supplicant/crypto_openssl.c
#LOCAL_C_INCLUDES:= \
#                   external/openssl/include
#LOCAL_MODULE := ultra_bcm_config
#LOCAL_STATIC_LIBRARIES := iwlib
#LOCAL_SHARED_LIBRARIES := libc libcutils libcrypto libssl
#LOCAL_CFLAGS := $(WT_INCS) $(WT_DEFS)
#include $(BUILD_EXECUTABLE)
#####################


