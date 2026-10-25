package com.sleekydz86.idolglow.groundedsearch.application.port

interface EmbeddingPort {
    fun modelName(): String

    fun embed(texts: List<String>): List<FloatArray>
}
