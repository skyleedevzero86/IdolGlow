package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.EventSearchProperties
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchDocument
import com.sleekydz86.idolglow.eventinfo.domain.EventSearchIndexOutcome
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.ObjectMapper
import java.time.Duration
import java.time.LocalDate
import java.util.UUID

@DisplayName("Elasticsearch 공연 검색 색인 (실제 Elasticsearch 8.15.3)")
class ElasticsearchEventSearchAdapterContainerTest {
    private val adapter: ElasticsearchEventSearchAdapter =
        adapter("http://${ELASTIC.host}:${ELASTIC.getMappedPort(PORT)}")

    @DisplayName("늦게 도착한 옛 버전은 검색 엔진이 거절하고, 새 버전 내용이 남는다")
    @Test
    fun olderVersionIsRejected() {
        // given
        adapter.index(문서("gid-a", "새 제목 공연", version = 2), overwriteSameVersion = false)

        // when
        val older = adapter.index(문서("gid-a", "옛 제목 공연", version = 1), overwriteSameVersion = false)
        adapter.refresh()

        // then
        assertEquals(EventSearchIndexOutcome.STALE, older)
        assertEquals(listOf("새 제목 공연"), adapter.search("공연", 10).map { it.title })
    }

    @DisplayName("같은 버전이 다시 오면 무시하고, 다시 색인할 때만 같은 버전을 덮어쓴다")
    @Test
    fun sameVersionIsIgnoredExceptReindex() {
        // given
        adapter.index(문서("gid-a", "회차 공연", version = 3), overwriteSameVersion = false)

        // when
        val duplicated = adapter.index(문서("gid-a", "회차 공연", version = 3), overwriteSameVersion = false)
        val reindexed = adapter.index(문서("gid-a", "회차 공연", version = 3), overwriteSameVersion = true)

        // then
        assertEquals(EventSearchIndexOutcome.STALE to EventSearchIndexOutcome.INDEXED, duplicated to reindexed)
    }

    @DisplayName("한국어 제목 일부와 번역 제목으로 찾고, 병합된 공연은 결과에서 뺀다")
    @Test
    fun findsByKoreanPartAndTranslationExceptMerged() {
        // given
        adapter.index(문서("gid-a", "연극 먼지여자", titles = mapOf("en" to "Dust Woman")), overwriteSameVersion = false)
        adapter.index(문서("gid-b", "연극 먼지여자", status = "MERGED"), overwriteSameVersion = false)
        adapter.refresh()

        // when
        val byKorean = adapter.search("먼지", 10)
        val byEnglish = adapter.search("dust", 10)

        // then
        assertEquals(listOf("gid-a"), byKorean.map { it.globalId })
        assertEquals(listOf("gid-a"), byEnglish.map { it.globalId })
        assertEquals(mapOf("en" to "Dust Woman"), byEnglish.single().titles)
        assertEquals(LocalDate.of(2026, 12, 1), byEnglish.single().startDate)
    }

    @DisplayName("검색 엔진에 연결하지 못하면 한국어 메시지로 실패를 알린다")
    @Test
    fun unreachableEngineFails() {
        // given
        val down = adapter("http://localhost:1")

        // when
        val error = assertThrows<IllegalStateException> { down.search("공연", 10) }

        // then
        assertTrue(error.message.orEmpty().startsWith("검색 엔진 연결 실패"), error.message)
    }

    private fun adapter(baseUrl: String): ElasticsearchEventSearchAdapter =
        ElasticsearchEventSearchAdapter(
            WebClient.create(),
            ObjectMapper(),
            EventSearchProperties(enabled = true, baseUrl = baseUrl, index = "idolglow-test-${UUID.randomUUID()}"),
        )

    private fun 문서(
        globalId: String,
        title: String,
        version: Long = 1,
        titles: Map<String, String> = emptyMap(),
        status: String = "ACTIVE",
    ): EventSearchDocument =
        EventSearchDocument(
            globalId = globalId,
            title = title,
            titles = titles,
            venue = "세종문화회관",
            startDate = LocalDate.of(2026, 12, 1),
            endDate = LocalDate.of(2026, 12, 31),
            status = status,
            mergedInto = null,
            metaVersion = version,
        )

    companion object {
        private const val PORT: Int = 9200

        private val ELASTIC: GenericContainer<*> =
            GenericContainer(DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:8.15.3"))
                .withEnv("discovery.type", "single-node")
                .withEnv("xpack.security.enabled", "false")
                .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m")
                .withExposedPorts(PORT)
                .waitingFor(Wait.forHttp("/_cluster/health").forPort(PORT).forStatusCode(200))
                .withStartupTimeout(Duration.ofMinutes(5))
                .apply { start() }
    }
}
