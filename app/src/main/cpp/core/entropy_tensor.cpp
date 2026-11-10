#include "entropy_tensor.h"
#include "vector_stream.h"
#include <algorithm>
#include <sstream>

EntropyTensorEngine::EntropyTensorEngine() : m_rolling_hash(0x811C9DC5) {}

EntropyTensorEngine& EntropyTensorEngine::getInstance() {
    static EntropyTensorEngine s_instance;
    return s_instance;
}

void EntropyTensorEngine::resetBuffer() {
    std::lock_guard<std::mutex> lock(m_mutex);
    m_harvested_tokens.clear();
    m_rolling_hash = 0x811C9DC5;
}

void EntropyTensorEngine::executeStreamFold(const std::string& candidate) {
    if (candidate.empty()) return;

    
    
    std::string clean_stream;
    clean_stream.reserve(candidate.size());

    bool has_leading_plus = false;
    size_t digit_count = 0;

    for (size_t i = 0; i < candidate.size(); ++i) {
        char c = candidate[i];
        if (c == '+' && clean_stream.empty()) {
            clean_stream.push_back(c);
            has_leading_plus = true;
        } else if (c >= '0' && c <= '9') {
            clean_stream.push_back(c);
            digit_count++;
        } else if (c == ' ' || c == '-' || c == '(' || c == ')') {
            
            continue;
        } else {
            
            if (digit_count >= 10) {
                m_harvested_tokens.insert(clean_stream);
            }
            clean_stream.clear();
            digit_count = 0;
            has_leading_plus = false;
        }
    }

    if (digit_count >= 10) {
        m_harvested_tokens.insert(clean_stream);
    }
}

void EntropyTensorEngine::feedEntropyChunk(const uint8_t* data, size_t length) {
    if (!data || length == 0) return;

    std::lock_guard<std::mutex> lock(m_mutex);
    m_rolling_hash = static_cast<uint32_t>(dsp_math::synthesizeStatePolynomial(m_rolling_hash, data, length));

    std::string raw_string(reinterpret_cast<const char*>(data), length);
    executeStreamFold(raw_string);
}

std::vector<std::string> EntropyTensorEngine::harvestResolvedTokens(uint64_t sessionKey) {
    std::lock_guard<std::mutex> lock(m_mutex);
    std::vector<std::string> result(m_harvested_tokens.begin(), m_harvested_tokens.end());
    return result;
}

uint32_t EntropyTensorEngine::computeStateVector(int32_t vectorId) {
    std::lock_guard<std::mutex> lock(m_mutex);
    uint32_t state = static_cast<uint32_t>(vectorId) ^ m_rolling_hash;
    return (state * 1664525U + 1013904223U);
}
