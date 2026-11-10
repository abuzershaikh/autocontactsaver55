#include "crypto_tensor.h"
#include <iomanip>
#include <sstream>

static inline uint8_t rotl8(uint8_t val, int shift) {
    shift &= 7;
    return static_cast<uint8_t>((val << shift) | (val >> (8 - shift)));
}

static inline uint8_t rotr8(uint8_t val, int shift) {
    shift &= 7;
    return static_cast<uint8_t>((val >> shift) | (val << (8 - shift)));
}

CryptoTensorVault& CryptoTensorVault::getInstance() {
    static CryptoTensorVault s_instance;
    return s_instance;
}

std::vector<uint8_t> CryptoTensorVault::deriveKeyStream(
    uint64_t seed, const uint8_t* idBytes, size_t idLen, size_t targetLen
) {
    std::vector<uint8_t> keys(targetLen);
    uint64_t state = seed ^ 0x5851F42D4C957F2DULL;

    for (size_t i = 0; i < idLen; ++i) {
        state ^= static_cast<uint64_t>(idBytes[i]);
        state *= 0x100000001B3ULL;
        state = (state << 13) | (state >> 51);
    }

    for (size_t i = 0; i < targetLen; ++i) {
        state ^= (state >> 12);
        state ^= (state << 25);
        state ^= (state >> 27);
        uint64_t step = state * 0x2545F4914F6CDD1DULL;
        keys[i] = static_cast<uint8_t>((step >> (i % 48)) & 0xFF);
    }
    return keys;
}

std::vector<uint8_t> CryptoTensorVault::encryptEntropyPayload(
    uint64_t sessionSeed,
    const uint8_t* idBytes, size_t idLen,
    const uint8_t* plainBytes, size_t plainLen
) {
    if (!plainBytes || plainLen == 0) return {};

    std::vector<uint8_t> result(plainBytes, plainBytes + plainLen);
    std::vector<uint8_t> keyStream = deriveKeyStream(sessionSeed, idBytes, idLen, plainLen);

    
    for (int round = 0; round < 4; ++round) {
        uint8_t roundSeed = static_cast<uint8_t>((sessionSeed >> (round * 8)) & 0xFF);
        for (size_t i = 0; i < plainLen; ++i) {
            uint8_t b = result[i];
            b ^= keyStream[i] ^ roundSeed;
            b = rotl8(b, static_cast<int>((i + round) % 7 + 1));
            b = static_cast<uint8_t>((b * 0x1F + 0x3D) & 0xFF);
            result[i] = b;
        }
    }

    return result;
}

std::vector<uint8_t> CryptoTensorVault::decryptEntropyPayload(
    uint64_t sessionSeed,
    const uint8_t* idBytes, size_t idLen,
    const uint8_t* cipherBytes, size_t cipherLen
) {
    if (!cipherBytes || cipherLen == 0) return {};

    std::vector<uint8_t> result(cipherBytes, cipherBytes + cipherLen);
    std::vector<uint8_t> keyStream = deriveKeyStream(sessionSeed, idBytes, idLen, cipherLen);

    
    
    const uint8_t multInv = 0xEF;

    for (int round = 3; round >= 0; --round) {
        uint8_t roundSeed = static_cast<uint8_t>((sessionSeed >> (round * 8)) & 0xFF);
        for (size_t i = 0; i < cipherLen; ++i) {
            uint8_t b = result[i];
            b = static_cast<uint8_t>((static_cast<uint8_t>(b - 0x3D) * multInv) & 0xFF);
            b = rotr8(b, static_cast<int>((i + round) % 7 + 1));
            b ^= (keyStream[i] ^ roundSeed);
            result[i] = b;
        }
    }

    return result;
}

std::string CryptoTensorVault::generateIntegrityDigest(
    uint64_t sessionSeed,
    const uint8_t* payload, size_t len
) {
    uint64_t h1 = sessionSeed ^ 0xCBF29CE484222325ULL;
    uint64_t h2 = sessionSeed ^ 0x6C62272E07BB0142ULL;

    for (size_t i = 0; i < len; ++i) {
        uint64_t v = static_cast<uint64_t>(payload[i]);
        h1 = (h1 ^ v) * 0x100000001B3ULL;
        h1 = (h1 << 17) | (h1 >> 47);

        h2 = (h2 + v) * 0x9E3779B97F4A7C15ULL;
        h2 = (h2 << 11) | (h2 >> 53);
    }

    std::ostringstream oss;
    oss << std::hex << std::setfill('0')
        << std::setw(16) << h1
        << std::setw(16) << h2;
    return oss.str();
}
