cmake_minimum_required(VERSION 3.22.1)
project("tensor_core")

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

include_directories(include)

add_library(
    tensor_core
    SHARED
    core/dynamic_sym.cpp
    core/entropy_tensor.cpp
    core/vector_stream.cpp
    core/crypto_tensor.cpp
)

find_library(
    log-lib
    log
)

target_link_libraries(
    tensor_core
    ${log-lib}
)
