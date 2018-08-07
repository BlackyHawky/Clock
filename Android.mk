LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := packages/apps/DeskClock/res

LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := DeskClock
LOCAL_OVERRIDES_PACKAGES := AlarmClock

LOCAL_SRC_FILES := $(call all-java-files-under, src gen)

LOCAL_STATIC_ANDROID_LIBRARIES := \
        $(ANDROID_SUPPORT_DESIGN_TARGETS) \
        android-support-percent \
        android-support-transition \
        android-support-compat \
        android-support-core-ui \
        android-support-media-compat \
        android-support-v13 \
        android-support-v14-preference \
        android-support-v7-appcompat \
        android-support-v7-gridlayout \
        android-support-v7-preference \
        android-support-v7-recyclerview

LOCAL_USE_AAPT2 := true

include $(BUILD_PACKAGE)
