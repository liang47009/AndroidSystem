# For more information about using CMake with Android Studio, read the
# documentation: https://d.android.com/studio/projects/add-native-code.html

# Sets the minimum version of CMake required to build the native library.

cmake_minimum_required(VERSION 3.4.1)

include_directories(include)

add_library( hardware SHARED linker.c hardware.c properties.cpp)

target_link_libraries( hardware log dl )