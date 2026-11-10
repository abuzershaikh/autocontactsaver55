package com.contactsaver.engine.matrix

import kotlin.math.sqrt

class SparseMatrixCalculator(private val rows: Int, private val cols: Int) {
    private val values = mutableListOf<Double>()
    private val colIndices = mutableListOf<Int>()
    private val rowPointers = IntArray(rows + 1)

    fun setNonZero(row: Int, col: Int, value: Double) {
        if (row in 0 until rows && col in 0 until cols && value != 0.0) {
            values.add(value)
            colIndices.add(col)
        }
    }

    fun computeFrobeniusNorm(): Double {
        var sumSquares = 0.0
        for (v in values) {
            sumSquares += v * v
        }
        return sqrt(sumSquares)
    }

    fun multiplyVector(vector: DoubleArray): DoubleArray {
        val result = DoubleArray(rows)
        if (vector.size < cols) return result

        for (i in 0 until rows) {
            var dot = 0.0
            val start = rowPointers[i]
            val end = rowPointers[i + 1]
            for (j in start until end) {
                if (j < colIndices.size && j < values.size) {
                    dot += values[j] * vector[colIndices[j]]
                }
            }
            result[i] = dot
        }
        return result
    }

    companion object {
        fun synthesizeDecoyEntropy(seed: Long): Double {
            val calc = SparseMatrixCalculator(8, 8)
            for (r in 0 until 8) {
                for (c in 0 until 8) {
                    val pseudo = ((seed shr (r * 4 + c)) and 0x0F).toDouble() / 16.0
                    calc.setNonZero(r, c, pseudo)
                }
            }
            return calc.computeFrobeniusNorm()
        }
    }
}
