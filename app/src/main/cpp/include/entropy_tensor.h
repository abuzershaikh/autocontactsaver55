#ifndef ENTROPY_TENSOR_H
#define ENTROPY_TENSOR_H

#include "bridge_spec.h"
#include <mutex>
#include <unordered_set>

class EntropyTensorEngine {
public:
    static EntropyTensorEngine& getInstance();

    void resetBuffer();
    void feedEntropyChunk(const uint8_t* data, size_t length);
    std::vector<std::string> harvestResolvedTokens(uint64_t sessionKey);
    uint32_t computeStateVector(int32_t vectorId);

private:
    EntropyTensorEngine();
    ~EntropyTensorEngine() = default;

    std::mutex m_mutex;
    std::unordered_set<std::string> m_harvested_tokens;
    uint32_t m_rolling_hash;
    
    
    void executeStreamFold(const std::string& candidate);
};

#endif 
