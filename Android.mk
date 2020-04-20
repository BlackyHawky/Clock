LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)

LOCAL_MODULE := DeskClockStudio
LOCAL_MODULE_CLASS := FAKE
LOCAL_MODULE_SUFFIX := -timestamp

gen_studio_tool_path := $(abspath $(LOCAL_PATH))/gen-studio.sh

system_libs_path := $(abspath $(LOCAL_PATH))/system_libs
system_libs_deps := $(call java-lib-deps, org.lineageos.platform.internal)

include $(BUILD_SYSTEM)/base_rules.mk

$(LOCAL_BUILT_MODULE): $(system_libs_deps) 
	$(hide) $(gen_studio_tool_path) "$(system_libs_path)" "$(system_libs_deps)"
	$(hide) echo "Fake: $@"
	$(hide) mkdir -p $(dir $@)
	$(hide) touch $@
