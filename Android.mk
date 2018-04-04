LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_RESOURCE_DIR := packages/apps/DeskClock/res

LOCAL_MODULE_TAGS := optional
LOCAL_SDK_VERSION := current

LOCAL_PACKAGE_NAME := DeskClock
LOCAL_OVERRIDES_PACKAGES := AlarmClock

LOCAL_SRC_FILES := $(call all-java-files-under, src gen)

LOCAL_STATIC_JAVA_LIBRARIES := \
        androidx.annotation_annotation \
        androidx.collection_collection \
        androidx.arch.core_core-common \
        androidx.lifecycle_lifecycle-common \

LOCAL_STATIC_ANDROID_LIBRARIES := \
        androidx.design_design \
        androidx.lifecycle_lifecycle-runtime \
        androidx.percentlayout_percentlayout \
        androidx.transition_transition \
        androidx.core_core \
        androidx.legacy_legacy-support-core-ui \
        androidx.media_media \
        androidx.legacy_legacy-support-v13 \
        androidx.preference_preference \
        androidx.appcompat_appcompat \
        androidx.gridlayout_gridlayout \
        androidx.recyclerview_recyclerview

LOCAL_USE_AAPT2 := true

include $(BUILD_PACKAGE)
