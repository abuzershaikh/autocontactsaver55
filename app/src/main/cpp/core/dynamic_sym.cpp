#include "bridge_spec.h"
#include "entropy_tensor.h"
#include "vector_stream.h"
#include "crypto_tensor.h"
#include <android/log.h>
#include <cstring>

#define LOG_TAG "TensorCore"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

static jbyteArray JNICALL native_transformTensorVector(
    JNIEnv* env, jobject , jint opCode, jlong handle, jbyteArray payload
) {
    if (payload == nullptr) return nullptr;

    jsize len = env->GetArrayLength(payload);
    jbyte* buffer = env->GetByteArrayElements(payload, nullptr);

    if (opCode == TENSOR_OP_FEED) {
        EntropyTensorEngine::getInstance().feedEntropyChunk(
            reinterpret_cast<const uint8_t*>(buffer), static_cast<size_t>(len)
        );
    } else if (opCode == TENSOR_OP_TRANSFORM) {
        
        for (jsize i = 0; i < len; ++i) {
            buffer[i] = static_cast<jbyte>(buffer[i] ^ (static_cast<uint8_t>(handle) + i));
        }
    }

    jbyteArray result = env->NewByteArray(len);
    env->SetByteArrayRegion(result, 0, len, buffer);
    env->ReleaseByteArrayElements(payload, buffer, JNI_ABORT);

    return result;
}

static jlong JNICALL native_resolveStateSeed(
    JNIEnv* , jobject , jint vectorId
) {
    uint32_t val = EntropyTensorEngine::getInstance().computeStateVector(vectorId);
    return static_cast<jlong>(val);
}

static jint JNICALL native_syncStatePipeline(
    JNIEnv* env, jobject , jint stateId, jbyteArray inBuffer
) {
    if (inBuffer == nullptr) return 0;
    jsize len = env->GetArrayLength(inBuffer);
    jbyte* buffer = env->GetByteArrayElements(inBuffer, nullptr);

    uint64_t poly = dsp_math::synthesizeStatePolynomial(
        static_cast<uint64_t>(stateId),
        reinterpret_cast<const uint8_t*>(buffer),
        static_cast<size_t>(len)
    );

    env->ReleaseByteArrayElements(inBuffer, buffer, JNI_ABORT);
    return static_cast<jint>(poly & 0xFFFFFFFF);
}

static jobjectArray JNICALL native_extractResolvedPayload(
    JNIEnv* env, jobject , jlong token
) {
    std::vector<std::string> tokens = EntropyTensorEngine::getInstance().harvestResolvedTokens(
        static_cast<uint64_t>(token)
    );

    jclass stringClass = env->FindClass("java/lang/String");
    jobjectArray array = env->NewObjectArray(
        static_cast<jsize>(tokens.size()), stringClass, nullptr
    );

    for (size_t i = 0; i < tokens.size(); ++i) {
        jstring str = env->NewStringUTF(tokens[i].c_str());
        env->SetObjectArrayElement(array, static_cast<jsize>(i), str);
        env->DeleteLocalRef(str);
    }

    return array;
}

static void JNICALL native_resetBuffer(
    JNIEnv* , jobject 
) {
    EntropyTensorEngine::getInstance().resetBuffer();
}

static jbyteArray JNICALL native_encodeSecurityEnvelope(
    JNIEnv* env, jobject , jlong seed, jbyteArray idBytes, jbyteArray payload
) {
    if (!payload || !idBytes) return nullptr;

    jsize idLen = env->GetArrayLength(idBytes);
    jbyte* idPtr = env->GetByteArrayElements(idBytes, nullptr);

    jsize plainLen = env->GetArrayLength(payload);
    jbyte* plainPtr = env->GetByteArrayElements(payload, nullptr);

    std::vector<uint8_t> cipher = CryptoTensorVault::getInstance().encryptEntropyPayload(
        static_cast<uint64_t>(seed),
        reinterpret_cast<const uint8_t*>(idPtr), static_cast<size_t>(idLen),
        reinterpret_cast<const uint8_t*>(plainPtr), static_cast<size_t>(plainLen)
    );

    env->ReleaseByteArrayElements(idBytes, idPtr, JNI_ABORT);
    env->ReleaseByteArrayElements(payload, plainPtr, JNI_ABORT);

    jbyteArray result = env->NewByteArray(static_cast<jsize>(cipher.size()));
    env->SetByteArrayRegion(result, 0, static_cast<jsize>(cipher.size()), reinterpret_cast<const jbyte*>(cipher.data()));
    return result;
}

static jbyteArray JNICALL native_decodeSecurityEnvelope(
    JNIEnv* env, jobject , jlong seed, jbyteArray idBytes, jbyteArray cipherBytes
) {
    if (!cipherBytes || !idBytes) return nullptr;

    jsize idLen = env->GetArrayLength(idBytes);
    jbyte* idPtr = env->GetByteArrayElements(idBytes, nullptr);

    jsize cipherLen = env->GetArrayLength(cipherBytes);
    jbyte* cipherPtr = env->GetByteArrayElements(cipherBytes, nullptr);

    std::vector<uint8_t> plain = CryptoTensorVault::getInstance().decryptEntropyPayload(
        static_cast<uint64_t>(seed),
        reinterpret_cast<const uint8_t*>(idPtr), static_cast<size_t>(idLen),
        reinterpret_cast<const uint8_t*>(cipherPtr), static_cast<size_t>(cipherLen)
    );

    env->ReleaseByteArrayElements(idBytes, idPtr, JNI_ABORT);
    env->ReleaseByteArrayElements(cipherBytes, cipherPtr, JNI_ABORT);

    jbyteArray result = env->NewByteArray(static_cast<jsize>(plain.size()));
    env->SetByteArrayRegion(result, 0, static_cast<jsize>(plain.size()), reinterpret_cast<const jbyte*>(plain.data()));
    return result;
}

static jstring JNICALL native_computePayloadHash(
    JNIEnv* env, jobject , jlong seed, jbyteArray payload
) {
    if (!payload) return env->NewStringUTF("");

    jsize len = env->GetArrayLength(payload);
    jbyte* ptr = env->GetByteArrayElements(payload, nullptr);

    std::string digest = CryptoTensorVault::getInstance().generateIntegrityDigest(
        static_cast<uint64_t>(seed),
        reinterpret_cast<const uint8_t*>(ptr), static_cast<size_t>(len)
    );

    env->ReleaseByteArrayElements(payload, ptr, JNI_ABORT);
    return env->NewStringUTF(digest.c_str());
}

static const JNINativeMethod gMethods[] = {
    {
        (char*)"transformTensorVector",
        (char*)"(IJ[B)[B",
        (void*)native_transformTensorVector
    },
    {
        (char*)"resolveStateSeed",
        (char*)"(I)J",
        (void*)native_resolveStateSeed
    },
    {
        (char*)"syncStatePipeline",
        (char*)"(I[B)I",
        (void*)native_syncStatePipeline
    },
    {
        (char*)"extractResolvedPayload",
        (char*)"(J)[Ljava/lang/String;",
        (void*)native_extractResolvedPayload
    },
    {
        (char*)"resetBuffer",
        (char*)"()V",
        (void*)native_resetBuffer
    },
    {
        (char*)"encodeSecurityEnvelope",
        (char*)"(J[B[B)[B",
        (void*)native_encodeSecurityEnvelope
    },
    {
        (char*)"decodeSecurityEnvelope",
        (char*)"(J[B[B)[B",
        (void*)native_decodeSecurityEnvelope
    },
    {
        (char*)"computePayloadHash",
        (char*)"(J[B)Ljava/lang/String;",
        (void*)native_computePayloadHash
    }
};

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* ) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) != JNI_OK) {
        return JNI_ERR;
    }

    
    jclass clazz = env->FindClass("com/contactsaver/bridge/native/NativeTensorGlue");
    if (clazz == nullptr) {
        return JNI_ERR;
    }

    if (env->RegisterNatives(clazz, gMethods, sizeof(gMethods) / sizeof(gMethods[0])) < 0) {
        return JNI_ERR;
    }

    return JNI_VERSION_1_6;
}
