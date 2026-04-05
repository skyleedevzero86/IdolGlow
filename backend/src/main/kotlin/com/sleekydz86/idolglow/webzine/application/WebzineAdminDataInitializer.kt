package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.webzine.domain.IssueCategory
import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSectionDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import com.sleekydz86.idolglow.webzine.infrastructure.WebzineIssueJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Profile("local", "dev")
@Component
class WebzineAdminDataInitializer(
    private val webzineIssueJpaRepository: WebzineIssueJpaRepository,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(WebzineAdminDataInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (webzineIssueJpaRepository.count() > 0) {
            return
        }

        val volume101 = createIssue(
            volume = 101,
            issueDate = LocalDate.of(2026, 3, 1),
            coverImageUrl = "https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1200&q=80",
            teaser = "전시, 공연, 포럼, 행사 콘텐츠가 하나의 호 안에서 어떻게 묶이는지 살펴보는 웹진 샘플 데이터입니다.",
        )
        addArticle(
            issue = volume101,
            slug = "language-of-being-beyond-fragments",
            title = "파편, 결핍이 아닌 존재의 언어로",
            kicker = "《파편의 파편: 박치호·정광희》전",
            summary = "부서진 조각을 결핍이 아니라 새로운 생성의 언어로 읽어내는 전시의 맥락을 풀어낸 기사입니다.",
            category = IssueCategory.EXHIBITION,
            formatLabel = "비디오",
            heroImageUrl = "https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1400&q=80",
            cardImageUrl = "https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=900&q=80",
            tags = listOf("설치미술", "파편", "박치호", "정광희", "동시대미술"),
            authorName = "소니영",
            authorEmail = "nayeongso@daum.net",
            creditLine = "Video 디자이너 양채영",
            highlightQuote = "\"당신은 어떤 문장을 적고 싶나요?\" 한 권의 책이 된 전시, 《파편의 파편》",
            galleryImageUrls = listOf(
                "https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1400&q=80",
                "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80",
                "https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80",
            ),
            sections = listOf(
                WebzineArticleSectionDraft(
                    heading = "요약정보",
                    body = "우리는 대개 부서지고 깨진 것을 불완전하다고 여긴다.\n\n하지만 전시는 파편을 오히려 새로운 가능성의 시작점으로 읽는다.",
                    note = null,
                ),
                WebzineArticleSectionDraft(
                    heading = "전시의 언어",
                    body = "정광희 작가에게 파편은 고정관념의 틀을 벗어나기 위한 의도적인 행위의 산물이다.\n\n작품은 파편이 흩어지는 순간 오히려 다른 가능성이 열린다는 감각을 전한다.",
                    note = "파편은 해체가 아니라 생성의 시작이라는 전시 해설.",
                ),
            ),
        )
        addArticle(
            issue = volume101,
            slug = "wednesday-theater-cultural-prescription",
            title = "한 주의 중심에서 만난 문화 처방전, ACC 수요극장",
            kicker = "공연 프로그램 큐레이션",
            summary = "수요극장이 한 주의 리듬을 어떻게 바꾸는지 공연 해설과 관람 포인트를 함께 담았습니다.",
            category = IssueCategory.PERFORMANCE,
            formatLabel = "아티클",
            heroImageUrl = "https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1400&q=80",
            cardImageUrl = "https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=900&q=80",
            tags = listOf("ACC수요극장", "라파치니의정원", "문화처방"),
            authorName = "ACC 편집부",
            authorEmail = "webzine@acc.or.kr",
            creditLine = "Photo ACC Archive",
            sections = listOf(
                WebzineArticleSectionDraft(
                    heading = "공연 리듬",
                    body = "수요극장은 관객의 한 주를 다시 묶어주는 반복 프로그램이다.\n\n공연 이후의 감상과 연결 콘텐츠까지 한 흐름으로 설계할 수 있다.",
                    note = null,
                ),
            ),
        )
        addArticle(
            issue = volume101,
            slug = "asia-garden-culture-program",
            title = "ACC 아시아 예술체험 <아시아의 정원문화>",
            kicker = "교육·포럼 프로그램",
            summary = "교육 체험 프로그램의 사진, 설명, 태그를 카드형 기사 구조로 정리한 샘플입니다.",
            category = IssueCategory.FORUM,
            formatLabel = "아티클",
            heroImageUrl = "https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=1400&q=80",
            cardImageUrl = "https://images.unsplash.com/photo-1466692476868-aef1dfb1e735?auto=format&fit=crop&w=900&q=80",
            tags = listOf("아시아예술체험", "정원문화", "교육"),
            authorName = "이수미",
            authorEmail = "academy@acc.or.kr",
            creditLine = "Photo Program Team",
            sections = listOf(
                WebzineArticleSectionDraft(
                    heading = "체험 포인트",
                    body = "아이들과 가족이 함께 참여할 수 있는 동선과 재료 구성이 중요하다.\n\n관리 화면에서는 카드 썸네일과 태그만으로도 핵심이 보여야 한다.",
                    note = null,
                ),
            ),
        )
        addArticle(
            issue = volume101,
            slug = "bridge-market-linking-people-and-culture",
            title = "사람과 문화를 잇는 다리, ACC 별별브릿지마켓",
            kicker = "행사·교류 현장 스케치",
            summary = "현장감이 강한 행사성 콘텐츠를 카드와 상세 본문으로 자연스럽게 이어주는 예시입니다.",
            category = IssueCategory.EVENT,
            formatLabel = "아티클",
            heroImageUrl = "https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=1400&q=80",
            cardImageUrl = "https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=900&q=80",
            tags = listOf("별별브릿지마켓", "문화교류", "현장스케치"),
            authorName = "김보라",
            authorEmail = "event@acc.or.kr",
            creditLine = "Photo Event Team",
            sections = listOf(
                WebzineArticleSectionDraft(
                    heading = "현장 분위기",
                    body = "현장 기사에서는 사진 비중과 카드 리스트에서의 시각적 임팩트가 중요하다.\n\n대표 이미지와 짧은 설명만으로도 행사의 성격이 드러나야 한다.",
                    note = null,
                ),
            ),
        )
        webzineIssueJpaRepository.save(volume101)

        val volume100 = createIssue(
            volume = 100,
            issueDate = LocalDate.of(2026, 1, 1),
            coverImageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80",
            teaser = "100호 특집을 위한 샘플 호입니다.",
        )
        addArticle(
            issue = volume100,
            slug = "artificial-you-exhibition",
            title = "인공지능이라는 거울 앞에 선 당신",
            kicker = "Vol.100 특집",
            summary = "강한 비주얼 중심의 특집 기사를 위한 관리자 샘플입니다.",
            category = IssueCategory.EXHIBITION,
            formatLabel = "비디오",
            heroImageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1400&q=80",
            cardImageUrl = "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=900&q=80",
            tags = listOf("ArtificialYou", "AI전시", "특집"),
            authorName = "ACC 편집부",
            authorEmail = "webzine@acc.or.kr",
            creditLine = "Design ACC",
            sections = listOf(
                WebzineArticleSectionDraft(
                    heading = "특집 개요",
                    body = "100호 특집은 강한 첫 화면과 카드 리스트의 통일된 톤이 중요하다.",
                    note = null,
                ),
            ),
        )
        webzineIssueJpaRepository.save(volume100)

        listOf(
            createIssue(99, LocalDate.of(2025, 12, 1), "https://images.unsplash.com/photo-1516321497487-e288fb19713f?auto=format&fit=crop&w=1200&q=80", "Vol.99 샘플 호입니다."),
            createIssue(98, LocalDate.of(2025, 11, 1), "https://images.unsplash.com/photo-1516321318423-f06f85e504b3?auto=format&fit=crop&w=1200&q=80", "Vol.98 샘플 호입니다."),
            createIssue(97, LocalDate.of(2025, 10, 1), "https://images.unsplash.com/photo-1489515217757-5fd1be406fef?auto=format&fit=crop&w=1200&q=80", "Vol.97 샘플 호입니다."),
        ).forEach(webzineIssueJpaRepository::save)

        log.info("웹진 ACC 관리자 샘플 데이터를 생성했습니다.")
    }

    private fun createIssue(
        volume: Int,
        issueDate: LocalDate,
        coverImageUrl: String,
        teaser: String,
    ): WebzineIssue = WebzineIssue.create(
        slug = "vol-$volume",
        volume = volume,
        issueDate = issueDate,
        coverImageUrl = coverImageUrl,
        teaser = teaser,
    )

    private fun addArticle(
        issue: WebzineIssue,
        slug: String,
        title: String,
        kicker: String,
        summary: String,
        category: IssueCategory,
        formatLabel: String,
        heroImageUrl: String,
        cardImageUrl: String,
        tags: List<String>,
        authorName: String,
        authorEmail: String,
        creditLine: String,
        sections: List<WebzineArticleSectionDraft>,
        highlightQuote: String? = null,
        galleryImageUrls: List<String> = emptyList(),
    ) {
        issue.addArticle(
            WebzineArticle.create(
                issue = issue,
                slug = slug,
                draft = WebzineArticleDraft(
                    title = title,
                    kicker = kicker,
                    summary = summary,
                    heroImageUrl = heroImageUrl,
                    cardImageUrl = cardImageUrl,
                    category = category,
                    formatLabel = formatLabel,
                    authorName = authorName,
                    authorEmail = authorEmail,
                    creditLine = creditLine,
                    highlightQuote = highlightQuote,
                    sections = sections,
                    galleryImageUrls = listOf(heroImageUrl, cardImageUrl) + galleryImageUrls,
                    tags = tags,
                )
            )
        )
    }
}
