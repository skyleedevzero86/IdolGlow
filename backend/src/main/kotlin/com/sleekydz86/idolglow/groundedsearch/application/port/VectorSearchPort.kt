package com.sleekydz86.idolglow.groundedsearch.application.port

interface VectorSearchPort {
    fun search(vector: FloatArray, limit: Int): List<Long>
}
