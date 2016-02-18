LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := packages/apps/DeskClock/res
LOCAL_RESOURCE_DIR += frameworks/opt/datetimepicker/res

ifeq ($(TARGET_BUILD_APPS),)
LOCAL_RESOURCE_DIR += frameworks/support/design/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/gridlayout/res
LOCAL_RESOURCE_DIR += frameworks/support/v7/recyclerview/res
else
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/design/res
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/appcompat/res
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/gridlayout/res
LOCAL_RESOURCE_DIR += prebuilts/sdk/current/support/v7/recyclerview/res
endif

LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := DeskClock
LOCAL_OVERRIDES_PACKAGES := AlarmClock

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := android-opt-datetimepicker
LOCAL_STATIC_JAVA_LIBRARIES += messageformat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-design
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-gridlayout
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-recyclerview

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.design
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.gridlayout
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.recyclerview
LOCAL_AAPT_FLAGS += --extra-packages com.android.datetimepicker
LOCAL_AAPT_FLAGS += --extra-packages com.android.messageformat

LOCAL_PROGUARD_FLAG_FILES := ../../../frameworks/support/design/proguard-rules.pro 

include $(BUILD_PACKAGE)
