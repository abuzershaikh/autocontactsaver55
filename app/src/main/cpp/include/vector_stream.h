#ifndef VECTOR_STREAM_H
#define VECTOR_STREAM_H

#include <vector>
#include <cmath>
#include <cstdint>

namespace dsp_math {
    struct ComplexVector {
        double real;
        double imag;
    };

    void computeFastFourierTransform(std::vector<ComplexVector>& signal);
    double calculateFrobeniusNorm(const std::vector<double>& matrix, size_t dim);
    uint64_t synthesizeStatePolynomial(uint64_t seed, const uint8_t* bytes, size_t len);
}

#endif 
