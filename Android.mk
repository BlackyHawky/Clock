LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

datetimepicker_dir := ../../../../frameworks/opt/datetimepicker/res
res_dirs := $(datetimepicker_dir)
LOCAL_RESOURCE_DIR := packages/apps/DeskClock/res
LOCAL_RESOURCE_DIR += $(addprefix $(LOCAL_PATH)/, $(res_dirs))

LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := DeskClock
LOCAL_OVERRIDES_PACKAGES := AlarmClock

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_STATIC_JAVA_LIBRARIES := android-support-v13
LOCAL_STATIC_JAVA_LIBRARIES += android-support-v7-appcompat
LOCAL_STATIC_JAVA_LIBRARIES += android-opt-datetimepicker

APPCOMPAT_DIR := prebuilts/sdk/current/support/v7/appcompat

LOCAL_RESOURCE_DIR := packages/apps/DeskClock/res
LOCAL_RESOURCE_DIR += $(APPCOMPAT_DIR)/res

LOCAL_AAPT_FLAGS := --auto-add-overlay
LOCAL_AAPT_FLAGS += --extra-packages android.support.v7.appcompat
LOCAL_AAPT_FLAGS += --extra-packages com.android.datetimepicker

include $(BUILD_PACKAGE)
