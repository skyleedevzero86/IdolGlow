package com.sleekydz86.idolglow.productpackage.product.adapter.web

import com.sleekydz86.idolglow.productpackage.product.application.ProductEventCalendar
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.UUID

@DisplayName("상품 이벤트 등록·수정·조회 API")
class ProductEventApiPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var context: WebApplicationContext

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var productEventCalendar: ProductEventCalendar

    private lateinit var mockMvc: MockMvc

    @BeforeEach
    fun setUp() {
        mockMvc =
            MockMvcBuilders
                .webAppContextSetup(context)
                .apply<DefaultMockMvcBuilder>(springSecurity())
                .build()
    }

    @DisplayName("이벤트 목록은 진행 중 상품만 종료일이 가까운 순, 같으면 ID 내림차순으로 돌려준다")
    @Test
    fun listsOnlyActiveEventsInOrder() {
        // given
        val tag = uniqueTag()
        val endsSoonFirst = create(tag, rateEvent(today(), today().plusDays(1)))
        val endsSoonSecond = create(tag, rateEvent(today().minusDays(2), today().plusDays(1)))
        val endsLater = create(tag, rateEvent(today().minusDays(1), today().plusDays(5)))
        create(tag, rateEvent(today().plusDays(1), today().plusDays(3)))
        create(tag, rateEvent(today().minusDays(3), today().minusDays(1)))
        create(tag, null)

        // when
        val ids = eventIds(tag)

        // then
        assertEquals(listOf(endsSoonSecond, endsSoonFirst, endsLater), ids)
    }

    @DisplayName("이벤트 목록 태그는 앞뒤 공백을 지우고 대소문자를 구분하지 않는다")
    @Test
    fun tagFilterIgnoresCaseAndSpaces() {
        // given
        val tag = uniqueTag()
        val id = create(tag, rateEvent(today(), today()))
        create(uniqueTag(), rateEvent(today(), today()))

        // when
        val ids = eventIds("  ${tag.uppercase()}  ")

        // then
        assertEquals(listOf(id), ids)
    }

    @DisplayName("이벤트 목록 size 가 1 미만이거나 100 초과면 400 BAD_REQUEST")
    @ParameterizedTest
    @ValueSource(ints = [0, 101])
    fun rejectsSizeOutOfRange(size: Int) {
        // when
        val result = mockMvc.perform(get("/products/events").param("size", size.toString()))

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("조회 개수는 1 이상 100 이하여야 합니다."))
    }

    @DisplayName("이벤트 목록 size 경계 1과 100은 받는다")
    @ParameterizedTest
    @ValueSource(ints = [1, 100])
    fun acceptsSizeBoundary(size: Int) {
        // when
        val result = mockMvc.perform(get("/products/events").param("size", size.toString()))

        // then
        result.andExpect(status().isOk)
    }

    @DisplayName("진행 중 이벤트 상품 상세는 이벤트 최저가와 총액을 함께 준다")
    @Test
    fun detailIncludesActiveEventPrice() {
        // given
        val id = create(uniqueTag(), rateEvent(today(), today().plusDays(1)))

        // when
        val result = mockMvc.perform(get("/products/$id"))

        // then
        result
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.event.status").value("ACTIVE"))
            .andExpect(jsonPath("$.event.discountRate").value(30))
            .andExpect(jsonPath("$.event.eventMinPrice").value(7000.0))
            .andExpect(jsonPath("$.event.eventTotalPrice").value(7000.0))
    }

    @DisplayName("이벤트 칸 없는 기존 등록 요청은 성공하고 이벤트 칸은 모두 비어 있다")
    @Test
    fun legacyCreateLeavesEventEmpty() {
        // when
        val id = create(uniqueTag(), null)

        // then
        assertEquals(0, eventColumnsFilled(id))
    }

    @DisplayName("수정에서 이벤트를 빼면 다섯 칸을 모두 비운다")
    @Test
    fun updateWithoutEventClearsColumns() {
        // given
        val id = create(uniqueTag(), fixedEvent(today(), today().plusDays(1), 5000))

        // when
        val result =
            mockMvc.perform(
                put("/admin/products/$id")
                    .with(user("1").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(productBody(uniqueTag(), null))),
            )

        // then
        result.andExpect(status().isNoContent)
        assertEquals(0, eventColumnsFilled(id))
    }

    @DisplayName("잘못된 이벤트 값은 400 BAD_REQUEST 와 한국어 메시지로 거절하고 상품을 만들지 않는다")
    @Test
    fun rejectsInvalidEvent() {
        // given
        val name = "잘못${UUID.randomUUID().toString().take(8)}"
        val body = productBody(uniqueTag(), rateEvent(today(), today().minusDays(1))) + ("name" to name)

        // when
        val result = postProduct(body)

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("이벤트 종료일은 시작일과 같거나 뒤여야 합니다."))
        assertEquals(
            0,
            jdbcTemplate.queryForObject("select count(*) from products where name = ?", Int::class.java, name),
        )
    }

    @DisplayName("수정 요청에서 상품 기본가를 내리면 이벤트 기본가는 새 기본가로 검사한다")
    @Test
    fun updateChecksFixedPriceAgainstNewBasePrice() {
        // given
        val id = create(uniqueTag(), null)
        val body = productBody(uniqueTag(), fixedEvent(today(), today(), 5000)) + ("basePrice" to 5000)

        // when
        val result =
            mockMvc.perform(
                put("/admin/products/$id")
                    .with(user("1").roles("ADMIN"))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(body)),
            )

        // then
        result
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errorCode").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("이벤트 기본가는 0원 이상이고 상품 기본가보다 작아야 합니다."))
        assertEquals(0, eventColumnsFilled(id))
    }

    private fun today(): LocalDate = productEventCalendar.today()

    private fun uniqueTag(): String = "tag${UUID.randomUUID().toString().take(8)}"

    private fun rateEvent(
        startsOn: LocalDate,
        endsOn: LocalDate,
    ): Map<String, Any> =
        mapOf(
            "startsOn" to startsOn.toString(),
            "endsOn" to endsOn.toString(),
            "priceType" to "DISCOUNT_RATE",
            "discountRate" to 30,
        )

    private fun fixedEvent(
        startsOn: LocalDate,
        endsOn: LocalDate,
        eventBasePrice: Int,
    ): Map<String, Any> =
        mapOf(
            "startsOn" to startsOn.toString(),
            "endsOn" to endsOn.toString(),
            "priceType" to "FIXED_BASE_PRICE",
            "eventBasePrice" to eventBasePrice,
        )

    private fun productBody(
        tag: String,
        event: Map<String, Any>?,
    ): Map<String, Any?> {
        val body =
            mapOf(
                "name" to "이벤트상품${UUID.randomUUID().toString().take(8)}",
                "description" to "설명",
                "basePrice" to 10000,
                "slotStartHour" to 9,
                "slotEndHour" to 16,
                "optionIds" to emptyList<Long>(),
                "tagNames" to listOf(tag),
                "tourAttractionPicks" to emptyList<Any>(),
            )
        return if (event == null) body else body + ("event" to event)
    }

    private fun postProduct(body: Map<String, Any?>) =
        mockMvc.perform(
            post("/products")
                .with(user("1").roles("ADMIN"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)),
        )

    private fun create(
        tag: String,
        event: Map<String, Any>?,
    ): Long {
        val response =
            postProduct(productBody(tag, event))
                .andExpect(status().isCreated)
                .andReturn()
                .response
                .contentAsString
        return objectMapper.readTree(response).get("id").asLong()
    }

    private fun eventIds(tag: String): List<Long> {
        val response =
            mockMvc
                .perform(get("/products/events").param("tag", tag).param("size", "100"))
                .andExpect(status().isOk)
                .andReturn()
                .response
                .contentAsString
        val items = objectMapper.readTree(response)
        return (0 until items.size()).map { items.get(it).get("id").asLong() }
    }

    private fun eventColumnsFilled(productId: Long): Int =
        jdbcTemplate.queryForObject(
            """
            select (case when event_starts_on is null then 0 else 1 end)
                + (case when event_ends_on is null then 0 else 1 end)
                + (case when event_price_type is null then 0 else 1 end)
                + (case when event_discount_rate is null then 0 else 1 end)
                + (case when event_base_price is null then 0 else 1 end)
            from products where id = ?
            """.trimIndent(),
            Int::class.java,
            productId,
        )!!
}
