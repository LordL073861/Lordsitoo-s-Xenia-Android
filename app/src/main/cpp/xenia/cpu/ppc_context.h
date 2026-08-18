#pragma once

#include <cstdint>
#include <cstring>

namespace xe::cpu {

// Xenon PowerPC 64-bit hardware registers structure
struct alignas(16) PPCContext {
    // 32 64-bit General Purpose Registers (r0 - r31)
    uint64_t r[32];

    // 32 64-bit Floating Point Registers (f0 - f31)
    double f[32];

    // 128 128-bit Vector (VMX / AltiVec) Registers (v0 - v127)
    struct alignas(16) VMXReg {
        union {
            uint32_t u32[4];
            uint16_t u16[8];
            uint8_t  u8[16];
            float    f32[4];
        };
    } v[128];

    // Special Purpose Registers (SPRs)
    uint64_t pc;       // Program Counter
    uint64_t lr;       // Link Register
    uint64_t ctr;      // Count Register
    uint32_t xer;      // Fixed-Point Exception Register
    uint32_t cr;       // Condition Register (CR0..CR7)
    uint64_t msr;      // Machine State Register
    uint32_t fpscr;    // Floating Point Status and Control Register
    uint32_t vscr;     // Vector Status and Control Register

    // Execution Thread state
    uint32_t thread_id;
    bool halted;

    void Reset() {
        std::memset(this, 0, sizeof(PPCContext));
        msr = 0x8000000000000000ULL; // 64-bit execution mode
    }
};

}  // namespace xe::cpu
