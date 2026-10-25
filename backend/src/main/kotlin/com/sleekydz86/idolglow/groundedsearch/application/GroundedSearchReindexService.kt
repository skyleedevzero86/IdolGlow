package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedChunkIndexPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedSourcePort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedSearchLimits
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import kotlin.math.min

@Service
class GroundedSearchReindexService(
    private val sourcePort: GroundedSourcePort,
    private val chunkIndexPort: GroundedChunkIndexPort,
    private val embeddingPort: EmbeddingPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun reindex(): GroundedReindexResult {
        val prepared = chunkIndexPort.prepare(sourcePort.readAll(), embeddingPort.modelName())
        if (prepared.toEmbed.isEmpty()) {
            return skipped(prepared.skipped)
        }
        val vectors = embedAll(prepared.toEmbed.map { it.content })
        chunkIndexPort.saveEmbeddings(prepared.toEmbed, vectors, embeddingPort.modelName())
        val result = GroundedReindexResult(embedded = prepared.toEmbed.size, skipped = prepared.skipped)
        log.info("근거 검색 색인 완료 embedded={} skipped={}", result.embedded, result.skipped)
        return result
    }

    private fun skipped(skipped: Int): GroundedReindexResult {
        log.info("근거 검색 색인 완료 embedded={} skipped={}", 0, skipped)
        return GroundedReindexResult(embedded = 0, skipped = skipped)
    }

    private fun embedAll(texts: List<String>): List<FloatArray> {
        val vectors = ArrayList<FloatArray>(texts.size)
        var start = 0
        val batch = GroundedSearchLimits.embedBatch()
        while (start < texts.size) {
            val end = min(start + batch, texts.size)
            vectors += embeddingPort.embed(texts.subList(start, end))
            start = end
        }
        if (vectors.size != texts.size) {
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "임베딩 개수가 문단 개수와 다릅니다.")
        }
        return vectors
    }
}
