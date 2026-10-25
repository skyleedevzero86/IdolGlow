package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import com.sleekydz86.idolglow.groundedsearch.application.port.VectorSearchPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedContentHash
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedVectorCodec
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunkEmbedding
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import com.sleekydz86.idolglow.groundedsearch.infrastructure.PostgresKeywordSearchAdapter
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkEmbeddingJpaRepository
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkJpaRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.math.BigDecimal

@DisplayName("근거 검색 PostgreSQL")
class GroundedSearchPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var keywordSearchPort: KeywordSearchPort

    @Autowired
    private lateinit var vectorSearchPort: VectorSearchPort

    @Autowired
    private lateinit var chunkRepository: RagChunkJpaRepository

    @Autowired
    private lateinit var embeddingRepository: RagChunkEmbeddingJpaRepository

    @Autowired
    private lateinit var reindexService: GroundedSearchReindexService

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    private var sequence = 1L

    @BeforeEach
    fun clearChunks() {
        sequence = 1L
        jdbcTemplate.execute("create extension if not exists pg_trgm")
        jdbcTemplate.update("delete from rag_chunk_embeddings")
        jdbcTemplate.update("delete from rag_chunks")
    }

    @Test
    @DisplayName("키워드 검색이 한국어 고유명사를 찾는다")
    fun keywordSearchFindsKoreanProperNoun() {
        // given
        assertTrue(keywordSearchPort is PostgresKeywordSearchAdapter)
        val match = saveChunk("홍대 소극장 메이크업 체험")
        saveChunk("강남 네일 아트")

        // when
        val ids = keywordSearchPort.search("홍대", keywordLimit())

        // then
        assertEquals(match.id, ids.first())
    }

    @Test
    @DisplayName("벡터 검색은 코사인 유사도 순서로 돌려준다")
    fun vectorSearchReturnsCosineOrder() {
        // given
        val near = saveChunk("가까운 문단")
        val middle = saveChunk("중간 문단")
        val far = saveChunk("먼 문단")
        saveVector(near, floatArrayOf(1f, 0f, 0f))
        saveVector(middle, floatArrayOf(1f, 1f, 0f))
        saveVector(far, floatArrayOf(0f, 1f, 0f))

        // when
        val ids = vectorSearchPort.search(floatArrayOf(1f, 0f, 0f), 2 + 1)

        // then
        assertEquals(listOf(near.id, middle.id, far.id), ids)
    }

    @Test
    @DisplayName("본문이 그대로면 다시 임베딩하지 않는다")
    fun reindexSkipsUnchangedChunks() {
        // given
        val product =
            productRepository.save(
                Product(name = "재색인 상품", description = "처음 설명", basePrice = BigDecimal.ONE),
            )
        reindexService.reindex()

        // when
        val second = reindexService.reindex()

        // then
        assertEquals(0, second.embedded)
        product.description = "바뀐 설명 토큰"
        productRepository.saveAndFlush(product)
        val third = reindexService.reindex()
        val chunk = chunkRepository.findBySourceTypeAndSourceId(RagSourceType.PRODUCT, product.id)
        assertTrue(third.embedded >= 1)
        assertNotNull(chunk)
        assertTrue(chunk!!.content.contains("바뀐 설명 토큰"))
        assertFalse(chunk.content.contains("09:00"))
    }

    private fun saveChunk(content: String): RagChunk {
        val sourceId = sequence
        sequence += 1L
        return chunkRepository.saveAndFlush(
            RagChunk(
                sourceType = RagSourceType.PRODUCT,
                sourceId = sourceId,
                linkKey = "product/$sourceId",
                content = content,
                contentHash = GroundedContentHash.of(content),
            ),
        )
    }

    private fun saveVector(chunk: RagChunk, vector: FloatArray) {
        embeddingRepository.saveAndFlush(
            RagChunkEmbedding(
                chunkId = chunk.id,
                model = "test-model",
                dims = vector.size,
                vectorBytes = GroundedVectorCodec.encode(vector),
                contentHash = chunk.contentHash,
            ),
        )
    }

    private fun keywordLimit(): Int = 2 + 2 + 1
}
