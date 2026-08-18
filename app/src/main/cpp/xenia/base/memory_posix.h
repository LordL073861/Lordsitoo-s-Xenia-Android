#pragma once

#include <cstdint>
#include <cstddef>
#include <sys/mman.h>

namespace xe {

class Memory {
public:
    Memory();
    ~Memory();

    bool Initialize();
    void Shutdown();

    uint8_t* virtual_membase() const { return virtual_membase_; }
    uint8_t* physical_membase() const { return physical_membase_; }

    uint8_t* Translate(uint32_t guest_address) const {
        if (!virtual_membase_) return nullptr;
        return virtual_membase_ + (guest_address & 0x1FFFFFFF);
    }

    bool Protect(uint32_t guest_address, size_t length, uint32_t protection);

    // Xbox 360 memory constants
    static constexpr size_t kGuestPhysicalMemorySize = 512 * 1024 * 1024; // 512MB UMA GDDR3
    static constexpr size_t kGuestVirtualAddressSpaceSize = 0x20000000;  // 512MB directly accessible window

private:
    uint8_t* virtual_membase_ = nullptr;
    uint8_t* physical_membase_ = nullptr;
    bool initialized_ = false;
};

}  // namespace xe
