package com.sleekydz86.idolglow.groundedsearch.domain

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.sqrt

object GroundedVectorCodec {
    fun encode(values: FloatArray): ByteArray {
        val buffer = ByteBuffer.allocate(values.size * Float.SIZE_BYTES).order(ByteOrder.LITTLE_ENDIAN)
        values.forEach(buffer::putFloat)
        return buffer.array()
    }

    fun decode(bytes: ByteArray, dims: Int): FloatArray {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        return FloatArray(dims) { buffer.float }
    }

    fun cosine(left: FloatArray, right: FloatArray): Double {
        val size = minOf(left.size, right.size)
        var dot = 0.0
        var leftNorm = 0.0
        var rightNorm = 0.0
        for (index in 0 until size) {
            val leftValue = left[index].toDouble()
            val rightValue = right[index].toDouble()
            dot += leftValue * rightValue
            leftNorm += leftValue * leftValue
            rightNorm += rightValue * rightValue
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0
        }
        return dot / (sqrt(leftNorm) * sqrt(rightNorm))
    }
}
