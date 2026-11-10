#include "vector_stream.h"
#include <cmath>

namespace dsp_math {

const double PI = 3.14159265358979323846;

void computeFastFourierTransform(std::vector<ComplexVector>& signal) {
    size_t n = signal.size();
    if (n <= 1) return;

    std::vector<ComplexVector> even(n / 2);
    std::vector<ComplexVector> odd(n / 2);

    for (size_t i = 0; i < n / 2; ++i) {
        even[i] = signal[i * 2];
        odd[i] = signal[i * 2 + 1];
    }

    computeFastFourierTransform(even);
    computeFastFourierTransform(odd);

    for (size_t k = 0; k < n / 2; ++k) {
        double angle = -2.0 * PI * k / n;
        ComplexVector t = {
            std::cos(angle) * odd[k].real - std::sin(angle) * odd[k].imag,
            std::cos(angle) * odd[k].imag + std::sin(angle) * odd[k].real
        };

        signal[k] = { even[k].real + t.real, even[k].imag + t.imag };
        signal[k + n / 2] = { even[k].real - t.real, even[k].imag - t.imag };
    }
}

double calculateFrobeniusNorm(const std::vector<double>& matrix, size_t dim) {
    double sum = 0.0;
    for (size_t i = 0; i < dim * dim && i < matrix.size(); ++i) {
        sum += matrix[i] * matrix[i];
    }
    return std::sqrt(sum);
}

uint64_t synthesizeStatePolynomial(uint64_t seed, const uint8_t* bytes, size_t len) {
    uint64_t hash = seed ^ 0xCBF29CE484222325ULL;
    for (size_t i = 0; i < len; ++i) {
        hash ^= static_cast<uint64_t>(bytes[i]);
        hash *= 0x100000001B3ULL; 
        hash = (hash << 7) | (hash >> 57);
    }
    return hash;
}

} 
