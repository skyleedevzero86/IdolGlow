package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.security.MessageDigest

@Component
@Profile("test")
class FakeGroundedEmbeddingClient : EmbeddingPort {
    override fun modelName(): String = MODEL

    override fun embed(texts: List<String>): List<FloatArray> = texts.map(::vectorOf)

    private fun vectorOf(text: String): FloatArray {
        val digest = MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
        return FloatArray(digest.size) { index -> digest[index].toFloat() }
    }

    private companion object {
        private const val MODEL = "fake-grounded-search"
    }
}
