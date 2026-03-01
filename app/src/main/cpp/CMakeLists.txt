cmake_minimum_required(VERSION 3.22.1)
project(monclia)

add_library(
    monclia-core
    SHARED
    pty_bridge.cpp
)

find_library(log-lib log)
find_library(android-lib android)

target_link_libraries(
    monclia-core
    ${log-lib}
    ${android-lib}
)
