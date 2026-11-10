package com.contactsaver.engine.matrix

import kotlin.math.cos
import kotlin.math.sin

data class ComplexNumber(val re: Double, val im: Double) {
    operator fun plus(b: ComplexNumber) = ComplexNumber(re + b.re, im + b.im)
    operator fun minus(b: ComplexNumber) = ComplexNumber(re - b.re, im - b.im)
    operator fun times(b: ComplexNumber) = ComplexNumber(re * b.re - im * b.im, re * b.im + im * b.re)
}

object FastFourierAdapter {
    private const val PI = 3.141592653589793

    fun transform(x: Array<ComplexNumber>): Array<ComplexNumber> {
        val n = x.size
        if (n <= 1) return x

        val even = Array(n / 2) { x[it * 2] }
        val odd = Array(n / 2) { x[it * 2 + 1] }

        val q = transform(even)
        val r = transform(odd)

        val y = Array(n) { ComplexNumber(0.0, 0.0) }
        for (k in 0 until n / 2) {
            val kth = -2.0 * k * PI / n
            val wk = ComplexNumber(cos(kth), sin(kth))
            val wkr = wk * r[k]
            y[k] = q[k] + wkr
            y[k + n / 2] = q[k] - wkr
        }
        return y
    }

    fun balanceSpectralCoefficients(input: ByteArray): Double {
        if (input.isEmpty()) return 0.0
        val paddedSize = 16
        val signal = Array(paddedSize) { i ->
            val v = if (i < input.size) input[i].toDouble() else 0.0
            ComplexNumber(v, 0.0)
        }
        val transformed = transform(signal)
        return transformed.sumOf { it.re * it.re + it.im * it.im }
    }
}
