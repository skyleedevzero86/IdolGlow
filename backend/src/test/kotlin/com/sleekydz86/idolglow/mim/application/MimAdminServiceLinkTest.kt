package com.sleekydz86.idolglow.mim.application

import com.sleekydz86.idolglow.mim.application.dto.UpsertMimRequest
import com.sleekydz86.idolglow.mim.domain.MimItem
import com.sleekydz86.idolglow.mim.domain.MimListCriteria
import com.sleekydz86.idolglow.mim.domain.MimRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("메인 슬라이드 이동 경로")
class MimAdminServiceLinkTest {
    @Test
    @DisplayName("앞에 슬래시가 없으면 사이트 경로로 저장한다")
    fun storesSitePathWithLeadingSlash() {
        // given
        val service = MimAdminService(RecordingMimRepository())

        // when
        val saved = service.create(request("articles/article-013"))

        // then
        assertThat(saved.linkUrl).isEqualTo("/articles/article-013")
    }

    @Test
    @DisplayName("외부 주소는 입력한 그대로 저장한다")
    fun keepsExternalUrl() {
        // given
        val service = MimAdminService(RecordingMimRepository())

        // when
        val saved = service.create(request("https://example.com/show"))

        // then
        assertThat(saved.linkUrl).isEqualTo("https://example.com/show")
    }

    @Test
    @DisplayName("빈 경로는 저장하지 않는다")
    fun storesBlankPathAsNull() {
        // given
        val service = MimAdminService(RecordingMimRepository())

        // when
        val saved = service.create(request("  "))

        // then
        assertThat(saved.linkUrl).isNull()
    }

    @Test
    @DisplayName("분류는 메인, 이벤트, 할인, 공지만 저장한다")
    fun storesOnlyKnownCategories() {
        // given
        val service = MimAdminService(RecordingMimRepository())

        // when
        val discount = service.create(request("/glow", "할인"))
        val unknown = service.create(request("/glow", "기타"))

        // then
        assertThat(discount.categoryLabel).isEqualTo("할인")
        assertThat(unknown.categoryLabel).isEqualTo("메인")
    }

    private fun request(
        linkUrl: String,
        categoryLabel: String? = null,
    ): UpsertMimRequest =
        UpsertMimRequest(
            imageName = "아이 휴먼",
            imagePath = "https://cdn.example/slide.jpg",
            imageFileName = "slide.jpg",
            linkUrl = linkUrl,
            categoryLabel = categoryLabel,
            description = null,
            activeYn = "Y",
            createdBy = "admin",
        )
}

private class RecordingMimRepository : MimRepository {
    private var stored: MimItem? = null

    override fun findList(criteria: MimListCriteria): List<MimItem> = emptyList()

    override fun count(criteria: MimListCriteria): Int = 0

    override fun findById(imageId: String): MimItem? = stored

    override fun findActiveByDomain(domainId: String): List<MimItem> = emptyList()

    override fun insert(item: MimItem) {
        stored = item
    }

    override fun update(item: MimItem) {
        stored = item
    }

    override fun delete(imageId: String) {
        stored = null
    }
}
