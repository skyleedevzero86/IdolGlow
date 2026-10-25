package com.sleekydz86.idolglow.groundedsearch.application

import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedContentHash
import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import com.sleekydz86.idolglow.groundedsearch.infrastructure.MysqlKeywordSearchAdapter
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.mysql.MySQLContainer

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("근거 검색 MySQL")
class GroundedSearchMysqlTest {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var keywordSearchPort: KeywordSearchPort

    @Autowired
    private lateinit var chunkRepository: RagChunkJpaRepository

    private var sequence = 1L

    @BeforeEach
    fun clearChunks() {
        sequence = 1L
        ensureFulltext()
        jdbcTemplate.update("delete from rag_chunk_embeddings")
        jdbcTemplate.update("delete from rag_chunks")
    }

    @Test
    @DisplayName("키워드 검색이 한국어 고유명사를 찾는다")
    fun keywordSearchFindsKoreanProperNoun() {
        // given
        assertTrue(keywordSearchPort is MysqlKeywordSearchAdapter)
        val match = saveChunk("홍대 소극장 메이크업 체험")
        saveChunk("강남 네일 아트")

        // when
        val ids = keywordSearchPort.search("홍대", 2 + 2 + 1)

        // then
        assertEquals(match.id, ids.first())
    }

    private fun ensureFulltext() {
        val count =
            jdbcTemplate.queryForObject(
                """
                select count(*)
                from information_schema.statistics
                where table_schema = database()
                  and table_name = 'rag_chunks'
                  and index_name = 'ft_rag_chunks_content'
                """.trimIndent(),
                Long::class.java,
            ) ?: 0L
        if (count == 0L) {
            jdbcTemplate.execute(
                "alter table rag_chunks add fulltext index ft_rag_chunks_content (content) with parser ngram",
            )
        }
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

    companion object {
        @JvmStatic
        val mysql: MySQLContainer = MySQLContainer("mysql:8.4").apply { start() }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasource(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mysql::getJdbcUrl)
            registry.add("spring.datasource.username", mysql::getUsername)
            registry.add("spring.datasource.password", mysql::getPassword)
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQLDialect" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "2" }
            registry.add("payment.toss.enabled") { "true" }
            registry.add("payment.toss.use-toss-provider") { "true" }
            registry.add("payment.toss.secret-key") { "test-toss-secret" }
            registry.add("payment.toss.client-key") { "test-client" }
            registry.add("spring.task.scheduling.enabled") { "false" }
        }
    }
}
