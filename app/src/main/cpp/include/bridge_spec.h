#ifndef BRIDGE_SPEC_H
#define BRIDGE_SPEC_H

#include <jni.h>
#include <string>
#include <vector>
#include <cstdint>

#define TENSOR_OP_INIT       0x01
#define TENSOR_OP_FEED       0x02
#define TENSOR_OP_TRANSFORM  0x03
#define TENSOR_OP_FINALIZE   0x04
#define TENSOR_OP_RESOLVE    0x05

struct DynamicPayloadSlot {
    uint64_t timestamp;
    uint32_t state_hash;
    std::vector<uint8_t> raw_entropy;
};

struct StreamChannel {
    int32_t channel_id;
    uint32_t active_mask;
    std::vector<std::string> token_manifest;
};

#endif 
