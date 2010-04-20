# Copyright (C) 2008 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

# vde_switch
LOCAL_ARM_MODE := arm

src_files :=  	src/common/open_memstream.c\
		src/common/canonicalize.c\
		src/vde_switch/consmgmt.c\
		src/vde_switch/datasock.c\
		src/vde_switch/fstp.c\
		src/vde_switch/hash.c\
		src/vde_switch/packetq.c\
		src/vde_switch/port.c\
		src/vde_switch/qtimer.c\
		src/vde_switch/sockutils.c\
		src/vde_switch/tuntap.c\
		src/vde_switch/vde_switch.c

LOCAL_SRC_FILES := $(src_files)
LOCAL_C_INCLUDES:= external/vde/include\
		external/vde/src/vde_switch\
		external/vde/src/common

LOCAL_MODULE := vde_switch 

LOCAL_SHARED_LIBRARIES := \
        libcutils \
        libutils

include $(BUILD_EXECUTABLE)

# slirpvde 
include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

src_files :=    src/common/open_memstream.c\
		src/common/canonicalize.c\
		src/lib/libvdeplug.c \
		src/slirpvde/bootp.c\
		src/slirpvde/cksum.c\
		src/slirpvde/debug.c\
		src/slirpvde/if.c\
		src/slirpvde/ip_icmp.c\
		src/slirpvde/ip_input.c\
		src/slirpvde/ip_output.c\
		src/slirpvde/mbuf.c\
		src/slirpvde/misc.c\
		src/slirpvde/sbuf.c\
		src/slirpvde/slirp.c\
		src/slirpvde/slirpvde.c\
		src/slirpvde/socket.c\
		src/slirpvde/tcp2unix.c\
		src/slirpvde/tcp_input.c\
		src/slirpvde/tcp_output.c\
		src/slirpvde/tcp_subr.c\
		src/slirpvde/tcp_timer.c\
		src/slirpvde/udp.c

LOCAL_SRC_FILES := $(src_files)
LOCAL_C_INCLUDES:= external/vde/include\
                external/vde/src/slirpvde\
		external/vde/src/lib\
                external/vde/src/common

LOCAL_MODULE := slirpvde 

LOCAL_SHARED_LIBRARIES := \
        libcutils \
        libutils

include $(BUILD_EXECUTABLE)

# vde_pcapplug

include $(CLEAR_VARS)

LOCAL_ARM_MODE := arm

src_files :=  	src/common/open_memstream.c\
		src/common/canonicalize.c\
		src/lib/libvdeplug.c \
		src/vde_pcapplug.c

LOCAL_SRC_FILES := $(src_files)
LOCAL_C_INCLUDES:= external/vde/include\
		external/vde/src\
		external/vde/src/common\
	        external/libpcap 


LOCAL_MODULE := vde_pcapplug 

LOCAL_STATIC_LIBRARIES+=libpcap

LOCAL_SHARED_LIBRARIES := \
        libcutils \
        libutils

include $(BUILD_EXECUTABLE)
