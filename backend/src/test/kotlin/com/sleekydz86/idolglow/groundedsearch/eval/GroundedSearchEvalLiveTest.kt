package com.sleekydz86.idolglow.groundedsearch.eval

import com.sleekydz86.idolglow.groundedsearch.application.port.ConditionExtractPort
import com.sleekydz86.idolglow.groundedsearch.application.port.EmbeddingPort
import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedHitPort
import com.sleekydz86.idolglow.groundedsearch.application.port.KeywordSearchPort
import com.sleekydz86.idolglow.groundedsearch.application.port.RerankPort
import com.sleekydz86.idolglow.groundedsearch.application.port.VectorSearchPort
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkEmbeddingJpaRepository
import com.sleekydz86.idolglow.groundedsearch.infrastructure.RagChunkJpaRepository
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.mysql.MySQLContainer

@SpringBootTest
@ActiveProfiles("eval")
@Tag("eval")
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = "\\S+")
@DisplayName("근거 검색 평가 MySQL")
class GroundedSearchEvalLiveTest {
    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var chunkRepository: RagChunkJpaRepository

    @Autowired
    private lateinit var embeddingRepository: RagChunkEmbeddingJpaRepository

    @Autowired
    private lateinit var embeddingPort: EmbeddingPort

    @Autowired
    private lateinit var keywordSearchPort: KeywordSearchPort

    @Autowired
    private lateinit var vectorSearchPort: VectorSearchPort

    @Autowired
    private lateinit var hitPort: GroundedHitPort

    @Autowired
    private lateinit var rerankPort: RerankPort

    @Autowired
    private lateinit var extractPort: ConditionExtractPort

    @Test
    @DisplayName("키가 있으면 MySQL에서 검색 방식별 점수를 측정한다")
    fun measuresWhenApiKeyPresent() {

        // given
        val cases = GroundedGoldenSet.cases()
        val probes = GroundedGoldenSet.probes()
        val chunks =
            GroundedEvalCorpus.prepare(cases, jdbcTemplate, chunkRepository, embeddingRepository, embeddingPort)
        val session =
            GroundedEvalSession(
                keywordSearchPort,
                vectorSearchPort,
                embeddingPort,
                hitPort,
                rerankPort,
                extractPort,
            )
        val rerank = !System.getenv("COHERE_API_KEY").isNullOrBlank()

        // when
        val result =
            GroundedEvalResult(
                database = "MySQL",
                skippedReason = if (rerank) null else "COHERE_API_KEY가 없어 Hybrid+Rerank는 측정하지 않았습니다.",
                modes = session.modes(cases, chunks, rerank),
                skippedModes = if (rerank) emptyList() else listOf(GroundedEvalReport.RERANK),
                fieldRates = session.fieldRates(cases),
                rejected = GroundedEvalValidation.rejectedCount(probes),
                probeCount = probes.size,
                caseCount = cases.size,
            )

        // then
        GroundedEvalReport.write(result)
    }

    companion object {
        private val mysql: MySQLContainer? =
            System.getenv("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }?.let {
                MySQLContainer("mysql:8.4").apply { start() }
            }

        @JvmStatic
        @DynamicPropertySource
        fun overrideDatasource(registry: DynamicPropertyRegistry) {
            val container = mysql ?: return
            registry.add("spring.datasource.url", container::getJdbcUrl)
            registry.add("spring.datasource.username", container::getUsername)
            registry.add("spring.datasource.password", container::getPassword)
            registry.add("spring.datasource.driver-class-name") { "com.mysql.cj.jdbc.Driver" }
            registry.add("spring.jpa.properties.hibernate.dialect") { "org.hibernate.dialect.MySQLDialect" }
            registry.add("spring.datasource.hikari.maximum-pool-size") { "2" }
            registry.add("spring.task.scheduling.enabled") { "false" }
            registry.add("payment.toss.enabled") { "true" }
            registry.add("payment.toss.use-toss-provider") { "true" }
            registry.add("payment.toss.secret-key") { "test-toss-secret" }
            registry.add("payment.toss.client-key") { "test-client" }
            registry.add("app.llm.openai.enabled") { "true" }
            registry.add("app.llm.openai.api-key") { System.getenv("OPENAI_API_KEY").orEmpty() }
            registry.add("app.llm.openai.base-url") { "https://api.openai.com" }
            registry.add("app.llm.openai.model") { "gpt-4.1-mini" }
            registry.add("app.grounded-search.llm.provider") { "openai" }
            registry.add("app.grounded-search.rerank.enabled") { rerankEnabled() }
            registry.add("app.grounded-search.rerank.api-key") { System.getenv("COHERE_API_KEY").orEmpty() }
        }

        private fun rerankEnabled(): String = if (System.getenv("COHERE_API_KEY").isNullOrBlank()) "false" else "true"
    }
}
