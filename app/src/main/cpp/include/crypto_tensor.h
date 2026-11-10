#ifndef CRYPTO_TENSOR_H
#define CRYPTO_TENSOR_H

#include <vector>
#include <string>
#include <cstdint>

class CryptoTensorVault {
public:
    static CryptoTensorVault& getInstance();

    std::vector<uint8_t> encryptEntropyPayload(
        uint64_t sessionSeed,
        const uint8_t* idBytes, size_t idLen,
        const uint8_t* plainBytes, size_t plainLen
    );

    std::vector<uint8_t> decryptEntropyPayload(
        uint64_t sessionSeed,
        const uint8_t* idBytes, size_t idLen,
        const uint8_t* cipherBytes, size_t cipherLen
    );

    std::string generateIntegrityDigest(
        uint64_t sessionSeed,
        const uint8_t* payload, size_t len
    );

private:
    CryptoTensorVault() = default;
    ~CryptoTensorVault() = default;

    std::vector<uint8_t> deriveKeyStream(uint64_t seed, const uint8_t* idBytes, size_t idLen, size_t targetLen);
};

#endif 
