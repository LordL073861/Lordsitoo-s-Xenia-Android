# CMakeLists.txt for Xenia-Android (Xenia Canary ARM64 Port)
cmake_minimum_required(VERSION 3.22.1)
project("xenia_app" C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Optimization and ARM64 flags
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -O3 -Wall -Wextra -Wno-unused-parameter -fexceptions -frtti")
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -march=armv8-a+crc+crypto -D__ARM_ARCH_8A__ -DXENIA_ARCH_ARM64=1")
endif()

add_definitions(
    -DXENIA_PLATFORM_ANDROID=1
    -DVK_USE_PLATFORM_ANDROID_KHR=1
    -DXENIA_DEBUG_LOGGING=1
)

include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/xenia
)

add_library(xenia_app SHARED
    xenia/base/logging.cc
    xenia/base/memory_posix.cc
    xenia/cpu/ppc_decoder.cc
    xenia/cpu/recompiler_arm64.cc
    xenia/gpu/vulkan/vulkan_device.cc
    xenia/gpu/vulkan/vulkan_swapchain.cc
    xenia/gpu/xenos.cc
    xenia/kernel/kernel_state.cc
    xenia/kernel/xex_module.cc
    xenia/vfs/stfs_container.cc
    xenia/apu/aaudio_driver.cc
    xenia/hid/android_input_driver.cc
    xenia/app/jni_bridge.cc
)

# Link essential Android NDK system libraries
find_library(log-lib log)
find_library(android-lib android)
find_library(vulkan-lib vulkan)
find_library(aaudio-lib aaudio)

target_link_libraries(xenia_app
    ${log-lib}
    ${android-lib}
    ${vulkan-lib}
    ${aaudio-lib}
)
