package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import com.sleekydz86.idolglow.glowcommunity.infrastructure.GlowCommunityPostJpaRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.translation.application.dto.LookupStatus
import com.sleekydz86.idolglow.translation.application.dto.TranslationLookupResult
import com.sleekydz86.idolglow.translation.application.dto.TranslationTarget
import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.EngineRequest
import com.sleekydz86.idolglow.translation.application.port.out.EngineResponse
import com.sleekydz86.idolglow.translation.application.port.out.JudgeRequest
import com.sleekydz86.idolglow.translation.application.port.out.TranslationEnginePort
import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationLocales
import com.sleekydz86.idolglow.translation.domain.TranslationProtector
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import com.sleekydz86.idolglow.translation.infrastructure.ContentTranslationJpaRepository
import com.sleekydz86.idolglow.translation.infrastructure.TranslationGlossaryTermJpaRepository
import com.sleekydz86.idolglow.translation.infrastructure.TranslationReportJpaRepository
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

@DisplayName("콘텐츠 번역 흐름")
class TranslationPipelinePostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var queryService: TranslationQueryService

    @Autowired
    private lateinit var scanService: TranslationScanService

    @Autowired
    private lateinit var txService: TranslationTxService

    @Autowired
    private lateinit var adminService: TranslationAdminService

    @Autowired
    private lateinit var reportService: TranslationReportService

    @Autowired
    private lateinit var contentTranslationPort: ContentTranslationPort

    @Autowired
    private lateinit var props: TranslationProperties

    @Autowired
    private lateinit var translationRepository: ContentTranslationJpaRepository

    @Autowired
    private lateinit var reportRepository: TranslationReportJpaRepository

    @Autowired
    private lateinit var glossaryRepository: TranslationGlossaryTermJpaRepository

    @Autowired
    private lateinit var productRepository: ProductJpaRepository

    @Autowired
    private lateinit var postRepository: GlowCommunityPostJpaRepository

    @Autowired
    private lateinit var userRepository: UserJpaRepository

    private val engine = FakeTranslationEngine()
    private var instant: Instant = Instant.parse("2026-11-15T00:00:00Z")

    @BeforeEach
    fun clean() {
        reportRepository.deleteAllInBatch()
        translationRepository.deleteAllInBatch()
        glossaryRepository.deleteAllInBatch()
        engine.available = true
        engine.translator = null
        engine.answers.clear()
        engine.judgeScore = 5
    }

    @AfterEach
    fun clearSecurity() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun aiOffKeepsWaitingAndShowsOriginal() {
        // given
        val product = product()
        engine.available = false

        // when
        val first = lookup("en", TranslationContentType.PRODUCT, product.id.toString())
        val summary = worker().runOnce()

        // then
        assertEquals(LookupStatus.PENDING, first.status)
        assertTrue(first.fields.isEmpty())
        assertFalse(summary.engineAvailable)
        assertEquals(
            setOf(TranslationStatus.PENDING),
            rows(TranslationContentType.PRODUCT, product.id.toString()).map { it.status }.toSet(),
        )
    }

    @Test
    fun officialTranslationIsShownOnlyAfterApproval() {
        // given
        val product = product()
        engine.answerAll(PRODUCT_TRANSLATIONS)
        lookup("en", TranslationContentType.PRODUCT, product.id.toString())

        // when
        worker().runOnce()
        val beforeApproval = lookup("en", TranslationContentType.PRODUCT, product.id.toString())
        val englishRow = row(TranslationContentType.PRODUCT, product.id.toString(), "en")
        authenticate(user(UserRole.ADMIN))
        adminService.approve(englishRow.id)
        val afterApproval = lookup("en", TranslationContentType.PRODUCT, product.id.toString())

        // then
        assertEquals(LookupStatus.ORIGINAL, beforeApproval.status)
        assertEquals(TranslationStatus.REVIEW, englishRow.status)
        assertTrue(englishRow.checkNotes.orEmpty().contains("review_required"))
        assertEquals(LookupStatus.TRANSLATED, afterApproval.status)
        assertEquals("en", afterApproval.resolvedLocale)
        assertTrue(afterApproval.machine)
        assertEquals("Seoul Night Tour", afterApproval.fields["name"])
        assertEquals(ENGLISH_DESCRIPTION, afterApproval.fields["description"])
    }

    @Test
    fun changedPriceNeverReachesUsers() {
        // given
        val product = product()
        engine.answerAll(
            mapOf(
                "en" to mapOf("name" to "Seoul Night Tour", "description" to "Namsan Tower course, price 5,500 KRW"),
                "ja" to mapOf("name" to "ソウル夜景ツアー", "description" to "南山タワー2時間コース、料金5,500ウォン"),
                "zh" to mapOf("name" to "首尔夜景之旅", "description" to "南山塔2小时路线，价格5,500韩元"),
            ),
        )
        lookup("en", TranslationContentType.PRODUCT, product.id.toString())
        val worker = worker()

        // when
        repeat(props.maxAttempts) {
            worker.runOnce()
            advance(Duration.ofDays(1))
        }
        val shown = lookup("en", TranslationContentType.PRODUCT, product.id.toString())
        val englishRow = row(TranslationContentType.PRODUCT, product.id.toString(), "en")

        // then
        assertEquals(TranslationStatus.FAILED, englishRow.status)
        assertTrue(englishRow.checkNotes.orEmpty().contains("number:description:55,000"))
        assertEquals(props.maxAttempts, englishRow.attempts)
        assertEquals(LookupStatus.ORIGINAL, shown.status)
    }

    @Test
    fun editedOrHiddenPostStopsServingOldTranslation() {
        // given
        val post = post("서울 야경 명소 추천", "남산타워 전망대가 제일 좋았어요")
        engine.answerAll(postTranslations("Seoul night view picks", "The Namsan Tower observatory was the best"))
        lookup("ja", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())
        worker().runOnce()
        val translatedBefore = lookup("ja", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // when
        post.edit(GlowCommunityTag.TALK, "서울 야경 명소 추천", "낙산공원 성곽길이 더 좋았어요")
        postRepository.saveAndFlush(post)
        scanService.scan()
        val afterEdit = lookup("ja", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())
        post.blind(java.time.LocalDateTime.now(), "테스트 블라인드")
        postRepository.saveAndFlush(post)
        scanService.scan()
        val afterBlind = lookup("ja", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // then
        assertEquals(LookupStatus.TRANSLATED, translatedBefore.status)
        assertEquals("ja", translatedBefore.resolvedLocale)
        assertEquals(LookupStatus.PENDING, afterEdit.status)
        assertTrue(afterEdit.fields.isEmpty())
        assertEquals(LookupStatus.ORIGINAL, afterBlind.status)
        assertEquals(
            setOf(TranslationStatus.SKIPPED),
            rows(TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString()).map { it.status }.toSet(),
        )
    }

    @Test
    fun japaneseFallsBackToEnglishWhileJapaneseIsNotReady() {
        // given
        val post = post("서울 야경 명소 추천", "남산타워 전망대가 제일 좋았어요")
        engine.answerAll(postTranslations("Seoul night view picks", "The Namsan Tower observatory was the best"))
        engine.answers["ja"] = mapOf("title" to "서울 야경 명소 추천", "content" to "남산타워 전망대가 제일 좋았어요")
        lookup("ja", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // when
        worker().runOnce()
        val shown = lookup("ja", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())
        val japaneseRow = row(TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString(), "ja")

        // then
        assertEquals(LookupStatus.TRANSLATED, shown.status)
        assertEquals("ja", shown.requestedLocale)
        assertEquals("en", shown.resolvedLocale)
        assertEquals("Seoul night view picks", shown.fields["title"])
        assertEquals(TranslationStatus.PENDING, japaneseRow.status)
        assertTrue(japaneseRow.checkNotes.orEmpty().contains("untranslated"))
    }

    @Test
    fun lowJudgeScoreGoesToReview() {
        // given
        val post = post("서울 야경 명소 추천", "남산타워 전망대가 제일 좋았어요")
        engine.answerAll(postTranslations("Seoul night view picks", "The Namsan Tower observatory was the best"))
        engine.judgeScore = 2
        lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // when
        worker().runOnce()
        val englishRow = row(TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString(), "en")
        val shown = lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // then
        assertEquals(TranslationStatus.REVIEW, englishRow.status)
        assertEquals("judge:2", englishRow.checkNotes)
        assertEquals(LookupStatus.ORIGINAL, shown.status)
    }

    @Test
    fun reportsHideTranslationAfterThreshold() {
        // given
        val post = post("서울 야경 명소 추천", "남산타워 전망대가 제일 좋았어요")
        engine.answerAll(postTranslations("Seoul night view picks", "The Namsan Tower observatory was the best"))
        lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())
        worker().runOnce()
        val translationId =
            requireNotNull(lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString()).translationId)
        val reporters = (1..props.reportThreshold).map { user(UserRole.USER) }

        // when
        authenticate(reporters.first())
        reportService.report(translationId)
        val duplicate = reportService.report(translationId)
        val last =
            reporters.drop(1).map { reporter ->
                authenticate(reporter)
                reportService.report(translationId)
            }.last()
        val shown = lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // then
        assertEquals(1, duplicate.reportCount)
        assertTrue(last.hidden)
        assertEquals(props.reportThreshold, last.reportCount)
        assertEquals(TranslationStatus.REVIEW, requireNotNull(contentTranslationPort.findById(translationId)).status)
        assertEquals(LookupStatus.ORIGINAL, shown.status)
    }

    @Test
    fun koreanRequestShowsOriginalWithoutQueueing() {
        // given
        val product = product()

        // when
        val shown = lookup("ko", TranslationContentType.PRODUCT, product.id.toString())

        // then
        assertEquals(LookupStatus.ORIGINAL, shown.status)
        assertTrue(rows(TranslationContentType.PRODUCT, product.id.toString()).isEmpty())
    }

    @Test
    fun sourceEditedWhileTranslatingDropsStaleResult() {
        // given
        val post = post("서울 야경 명소 추천", "남산타워 전망대가 제일 좋았어요")
        var firstLocale = ""
        engine.translator = { request ->
            if (firstLocale.isEmpty()) {
                firstLocale = request.locale
                post.edit(GlowCommunityTag.TALK, "서울 야경 명소 추천", "낙산공원 성곽길이 더 좋았어요")
                postRepository.saveAndFlush(post)
                scanService.scan()
            }
            request.fields["content"]?.let { content ->
                val edited = content.contains("낙산공원")
                (if (edited) EDITED_POST_TRANSLATIONS else ORIGINAL_POST_TRANSLATIONS).getValue(request.locale)
            }
        }
        lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())
        val worker = worker()

        // when
        val firstRun = worker.runOnce()
        val raced = row(TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString(), firstLocale)
        advance(Duration.ofMinutes(1))
        worker.runOnce()
        val shown =
            TranslationLocales.TARGETS.associateWith { locale ->
                lookup(locale, TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())
            }

        // then
        assertEquals(1, firstRun.outcomes[TranslationTxService.OUTCOME_STALE])
        assertEquals(TranslationStatus.PENDING, raced.status)
        shown.forEach { (locale, result) ->
            assertEquals(LookupStatus.TRANSLATED, result.status)
            assertEquals(locale, result.resolvedLocale)
            assertEquals(EDITED_POST_TRANSLATIONS.getValue(locale)["content"], result.fields["content"])
        }
    }

    @Test
    fun linksSurviveTranslation() {
        // given
        val post = post("공연 후기", "예매는 [여기](https://idolglow.test/book) 에서 했어요")
        engine.translator = { request ->
            request.fields.mapValues { (field, value) ->
                if (field == "title") {
                    "Show review"
                } else {
                    value.replace("예매는", "I booked").replace("[여기]", "[here]").replace("에서 했어요", ".")
                }
            }
        }
        lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // when
        worker().runOnce()
        val shown = lookup("en", TranslationContentType.GLOW_COMMUNITY_POST, post.id.toString())

        // then
        assertEquals(LookupStatus.TRANSLATED, shown.status)
        assertEquals("I booked [here](https://idolglow.test/book) .", shown.fields["content"])
        assertFalse(engine.seen.any { it.fields.values.any { text -> text.contains("https://") } })
        assertTrue(engine.seen.any { it.fields.values.any { text -> text.contains(TranslationProtector.token(1)) } })
    }

    private fun worker(): TranslationWorkerService =
        TranslationWorkerService(contentTranslationPort, txService, engine, props).also { worker ->
            worker.clock = object : Clock() {
                override fun getZone(): ZoneId = ZoneId.of("Asia/Seoul")

                override fun withZone(zone: ZoneId): Clock = this

                override fun instant(): Instant = instant
            }
        }

    private fun advance(duration: Duration) {
        instant = instant.plus(duration)
    }

    private fun lookup(
        locale: String,
        type: TranslationContentType,
        key: String,
    ): TranslationLookupResult = queryService.lookup(locale, listOf(TranslationTarget(type, key))).single()

    private fun rows(
        type: TranslationContentType,
        key: String,
    ): List<ContentTranslation> = contentTranslationPort.findByTarget(type, key)

    private fun row(
        type: TranslationContentType,
        key: String,
        locale: String,
    ): ContentTranslation = rows(type, key).single { it.locale == locale }

    private fun product(): Product =
        productRepository.saveAndFlush(Product(name = "서울 야경 투어", description = "남산타워 2시간 코스, 가격 55,000원"))

    private fun post(
        title: String,
        content: String,
    ): GlowCommunityPost =
        postRepository.saveAndFlush(
            GlowCommunityPost.create(user(UserRole.USER).id, GlowCommunityTag.TALK, title, content),
        )

    private fun user(role: UserRole): User {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        return userRepository.save(User.of("tr$token@test.com", "t$token", role))
    }

    private fun authenticate(actor: User) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                actor.id.toString(),
                "",
                listOf(SimpleGrantedAuthority("ROLE_${actor.role.name}")),
            )
    }

    private fun postTranslations(
        title: String,
        content: String,
    ): Map<String, Map<String, String>> =
        mapOf(
            "en" to mapOf("title" to title, "content" to content),
            "ja" to mapOf("title" to "ソウル夜景スポットのおすすめ", "content" to "南山タワー展望台が一番よかったです"),
            "zh" to mapOf("title" to "首尔夜景推荐", "content" to "南山塔观景台最好"),
        )

    companion object {
        private const val NIGHT_VIEW_TITLE = "Seoul night view picks"
        private val ORIGINAL_POST_TRANSLATIONS =
            mapOf(
                "en" to mapOf("title" to NIGHT_VIEW_TITLE, "content" to "The Namsan Tower observatory was the best"),
                "ja" to mapOf("title" to "ソウル夜景スポットのおすすめ", "content" to "南山タワー展望台が一番よかったです"),
                "zh" to mapOf("title" to "首尔夜景推荐", "content" to "南山塔观景台最好"),
            )
        private val EDITED_POST_TRANSLATIONS =
            mapOf(
                "en" to mapOf("title" to NIGHT_VIEW_TITLE, "content" to "The Naksan Park fortress trail was better"),
                "ja" to mapOf("title" to "ソウル夜景スポットのおすすめ", "content" to "駱山公園の城郭道のほうがよかったです"),
                "zh" to mapOf("title" to "首尔夜景推荐", "content" to "骆山公园城墙路更好"),
            )
        private const val ENGLISH_DESCRIPTION = "Namsan Tower 2-hour course, price 55,000 KRW"
        private val PRODUCT_TRANSLATIONS =
            mapOf(
                "en" to mapOf("name" to "Seoul Night Tour", "description" to ENGLISH_DESCRIPTION),
                "ja" to mapOf("name" to "ソウル夜景ツアー", "description" to "南山タワー2時間コース、料金55,000ウォン"),
                "zh" to mapOf("name" to "首尔夜景之旅", "description" to "南山塔2小时路线，价格55,000韩元"),
            )
    }
}

private class FakeTranslationEngine : TranslationEnginePort {
    var available: Boolean = true
    var judgeScore: Int? = 5
    val answers: MutableMap<String, Map<String, String>> = mutableMapOf()
    var translator: ((EngineRequest) -> Map<String, String>?)? = null
    val seen: MutableList<EngineRequest> = mutableListOf()

    fun answerAll(byLocale: Map<String, Map<String, String>>) {
        translator = null
        answers.putAll(byLocale)
    }

    override fun available(): Boolean = available

    override fun translate(request: EngineRequest): EngineResponse? {
        seen += request
        val fields = translator?.invoke(request) ?: answers[request.locale] ?: return null
        return EngineResponse(fields, "fake")
    }

    override fun judge(request: JudgeRequest): Int? = judgeScore
}
