#include "memory_posix.h"
#include "logging.h"
#include <unistd.h>
#include <cerrno>
#include <cstring>

namespace xe {

Memory::Memory() = default;

Memory::~Memory() {
    Shutdown();
}

bool Memory::Initialize() {
    if (initialized_) return true;

    // Allocate 512 MB guest address space using mmap with PROT_READ | PROT_WRITE
    void* mapping = mmap(nullptr, kGuestPhysicalMemorySize,
                         PROT_READ | PROT_WRITE,
                         MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);

    if (mapping == MAP_FAILED) {
        XELOGE("Memory", "Failed to allocate 512MB guest memory space! errno=%d (%s)", errno, strerror(errno));
        return false;
    }

    virtual_membase_ = static_cast<uint8_t*>(mapping);
    physical_membase_ = virtual_membase_;
    initialized_ = true;

    XELOGI("Memory", "Initialized Xbox 360 512MB Unified Memory System at host %p", virtual_membase_);
    return true;
}

void Memory::Shutdown() {
    if (!initialized_) return;

    if (virtual_membase_) {
        munmap(virtual_membase_, kGuestPhysicalMemorySize);
        virtual_membase_ = nullptr;
        physical_membase_ = nullptr;
    }
    initialized_ = false;
    XELOGI("Memory", "Xbox 360 memory system shut down successfully.");
}

bool Memory::Protect(uint32_t guest_address, size_t length, uint32_t protection) {
    if (!initialized_) return false;
    uint8_t* ptr = Translate(guest_address);
    if (!ptr) return false;

    int prot = 0;
    if (protection & 1) prot |= PROT_READ;
    if (protection & 2) prot |= PROT_WRITE;
    if (protection & 4) prot |= PROT_EXEC;

    long page_size = sysconf(_SC_PAGESIZE);
    uintptr_t addr = reinterpret_cast<uintptr_t>(ptr);
    uintptr_t aligned_addr = addr & ~(page_size - 1);
    size_t aligned_length = length + (addr - aligned_addr);

    return mprotect(reinterpret_cast<void*>(aligned_addr), aligned_length, prot) == 0;
}

}  // namespace xe
