#pragma once

#include <cstdint>
#include <cstring>

namespace xe {

inline uint16_t byte_swap(uint16_t value) {
    return __builtin_bswap16(value);
}

inline uint32_t byte_swap(uint32_t value) {
    return __builtin_bswap32(value);
}

inline uint64_t byte_swap(uint64_t value) {
    return __builtin_bswap64(value);
}

inline float byte_swap(float value) {
    uint32_t temp;
    std::memcpy(&temp, &value, sizeof(temp));
    temp = byte_swap(temp);
    float result;
    std::memcpy(&result, &temp, sizeof(result));
    return result;
}

inline double byte_swap(double value) {
    uint64_t temp;
    std::memcpy(&temp, &value, sizeof(temp));
    temp = byte_swap(temp);
    double result;
    std::memcpy(&result, &temp, sizeof(result));
    return result;
}

template <typename T>
inline T load_be(const void* ptr) {
    T val;
    std::memcpy(&val, ptr, sizeof(T));
    return byte_swap(val);
}

template <typename T>
inline void store_be(void* ptr, T val) {
    T swapped = byte_swap(val);
    std::memcpy(ptr, &swapped, sizeof(T));
}

}  // namespace xe
