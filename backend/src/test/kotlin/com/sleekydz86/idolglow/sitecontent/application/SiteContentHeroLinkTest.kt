package com.sleekydz86.idolglow.sitecontent.application

import com.sleekydz86.idolglow.bnr.domain.BnrItem
import com.sleekydz86.idolglow.bnr.domain.BnrListCriteria
import com.sleekydz86.idolglow.bnr.domain.BnrRepository
import com.sleekydz86.idolglow.global.config.AppPublicUrlProperties
import com.sleekydz86.idolglow.mim.domain.MimItem
import com.sleekydz86.idolglow.mim.domain.MimListCriteria
import com.sleekydz86.idolglow.mim.domain.MimRepository
import com.sleekydz86.idolglow.pup.domain.PupItem
import com.sleekydz86.idolglow.pup.domain.PupListCriteria
import com.sleekydz86.idolglow.pup.domain.PupRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("메인 슬라이드 공개 경로")
class SiteContentHeroLinkTest {
    @Test
    @DisplayName("등록한 경로와 분류를 메인에 보여 준다")
    fun usesRegisteredPathAndCategory() {
        // given
        val query = service(slide(linkUrl = "/glow"))

        // when
        val home = query.readHomeContent()

        // then
        assertThat(home.heroSlides.single().linkUrl).isEqualTo("/glow")
        assertThat(home.heroSlides.single().categoryLabel).isEqualTo("이벤트")
    }

    @Test
    @DisplayName("경로가 없으면 기사 목록으로 연다")
    fun opensArticlesWhenPathIsMissing() {
        // given
        val query = service(slide(linkUrl = null))

        // when
        val home = query.readHomeContent()

        // then
        assertThat(home.heroSlides.single().linkUrl).isEqualTo("/articles")
    }

    @Test
    @DisplayName("이미지가 없는 진행 중 팝업도 홈에 포함한다")
    fun includesPopupWithoutImage() {
        // given
        val query =
            service(
                slide(linkUrl = "/glow"),
                listOf(popupWithoutImage()),
            )

        // when
        val home = query.readHomeContent()

        // then
        assertThat(home.popups).hasSize(1)
        assertThat(home.popups.single().title).isEqualTo("안내 팝업")
        assertThat(home.popups.single().imageUrl).isNull()
    }

    private fun service(
        item: MimItem,
        popups: List<PupItem> = emptyList(),
    ): SiteContentQueryService =
        SiteContentQueryService(
            bnrRepository = EmptyBnrRepository(),
            pupRepository = FixedPupRepository(popups),
            mimRepository = FixedMimRepository(listOf(item)),
            appPublicUrlProperties = AppPublicUrlProperties(publicBaseUrl = "http://localhost:8080"),
        )

    private fun slide(linkUrl: String?): MimItem =
        MimItem(
            imageId = "IMG_1",
            domainId = "kr",
            imageName = "아이 휴먼",
            imagePath = "https://cdn.example/slide.jpg",
            imageFileName = "slide.jpg",
            linkUrl = linkUrl,
            categoryLabel = "이벤트",
            description = null,
            activeYn = "Y",
            createdBy = null,
            createdAtFormatted = null,
            domainName = null,
        )

    private fun popupWithoutImage(): PupItem =
        PupItem(
            popupId = "POP_1",
            domainId = "kr",
            title = "안내 팝업",
            fileUrl = null,
            linkTarget = "_blank",
            imagePath = null,
            imageFileName = null,
            noticeStartDate = null,
            noticeEndDate = null,
            stopViewYn = "Y",
            noticeYn = "Y",
            createdBy = null,
            createdAtFormatted = null,
            updatedBy = null,
            updatedAtFormatted = null,
            domainName = null,
        )
}

private class FixedMimRepository(
    private val items: List<MimItem>,
) : MimRepository {
    override fun findList(criteria: MimListCriteria): List<MimItem> = emptyList()

    override fun count(criteria: MimListCriteria): Int = 0

    override fun findById(imageId: String): MimItem? = items.firstOrNull { it.imageId == imageId }

    override fun findActiveByDomain(domainId: String): List<MimItem> = items

    override fun insert(item: MimItem) = Unit

    override fun update(item: MimItem) = Unit

    override fun delete(imageId: String) = Unit
}

private class EmptyBnrRepository : BnrRepository {
    override fun findList(criteria: BnrListCriteria): List<BnrItem> = emptyList()

    override fun count(criteria: BnrListCriteria): Int = 0

    override fun findById(bannerId: String): BnrItem? = null

    override fun findActiveByDomain(domainId: String): List<BnrItem> = emptyList()

    override fun insert(item: BnrItem) = Unit

    override fun update(item: BnrItem) = Unit

    override fun delete(bannerId: String) = Unit
}

private class FixedPupRepository(
    private val items: List<PupItem>,
) : PupRepository {
    override fun findList(criteria: PupListCriteria): List<PupItem> = emptyList()

    override fun count(criteria: PupListCriteria): Int = 0

    override fun findById(popupId: String): PupItem? = null

    override fun findPublicByDomain(domainId: String): List<PupItem> = items

    override fun insert(item: PupItem) = Unit

    override fun update(item: PupItem) = Unit

    override fun delete(popupId: String) = Unit
}
