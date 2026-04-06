package com.sleekydz86.idolglow.newsletter.application

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.newsletter.domain.NewsletterDraft
import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Profile("local", "dev")
@Component
class NewsletterAdminDataInitializer(
    private val newsletterRepository: NewsletterRepository,
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(NewsletterAdminDataInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (newsletterRepository.count() > 0) {
            return
        }

        val seedItems = listOf(
            newsletter(
                slug = "idol-glow-supporters-5th-launches-with-50-members",
                title = "Idol Glow 5기 서포터즈, 역대 최대 50명 활동 시작",
                publishedAt = LocalDate.of(2026, 3, 31),
                imageUrl = "https://images.unsplash.com/photo-1511578314322-379afb476865?auto=format&fit=crop&w=1200&q=80",
                tags = listOf("IdolGlow서포터즈", "문화브랜딩크루", "대학생기자단", "콘텐츠크루"),
                summary = "Idol Glow 5기 서포터즈가 발대식을 열고 전시, 공연, 교육, 온라인 콘텐츠 제작 활동을 본격 시작했습니다.",
                paragraphs = listOf(
                    "Idol Glow는 3월 말 5기 서포터즈 발대식을 열고 역대 최대 규모의 대학생 문화 서포터즈 활동을 시작했습니다.",
                    "서포터즈는 전시와 공연, 교육 프로그램 현장 취재와 SNS 기반 콘텐츠 제작에 참여하며 Idol Glow의 브랜드 경험을 확장하게 됩니다.",
                    "Idol Glow는 청년 기반 홍보 커뮤니티를 강화해 문화예술 경험을 일상 언어로 확산하는 구조를 지속적으로 만들 계획입니다.",
                ),
            ),
            newsletter(
                slug = "idol-glow-launches-specialized-cultural-forum",
                title = "Idol Glow, 9개 전문 분과로 문화협력 포럼 시작",
                publishedAt = LocalDate.of(2026, 3, 24),
                imageUrl = "https://images.unsplash.com/photo-1573164713988-8665fc963095?auto=format&fit=crop&w=1200&q=80",
                tags = listOf("IdolGlow포럼", "문화협력", "전문분과", "국제협력"),
                summary = "Idol Glow가 9개 전문 분과 체계로 문화협력 포럼을 새롭게 구성하고 실무 중심 논의를 시작했습니다.",
                paragraphs = listOf(
                    "이번 포럼은 전시, 공연, 교육, 국제교류 등 주요 문화 영역별 전문 분과로 구성됐습니다.",
                    "참여자들은 분야별 의제를 공유하며 Idol Glow가 앞으로 집중해야 할 문화 의제를 도출했습니다.",
                ),
            ),
            newsletter(
                slug = "idol-glow-stage-tour-behind-the-scenes",
                title = "Idol Glow 공연장 투어, 무대 뒤 숨은 설렘을 만나다",
                publishedAt = LocalDate.of(2026, 3, 18),
                imageUrl = "https://images.unsplash.com/photo-1503095396549-807759245b35?auto=format&fit=crop&w=1200&q=80",
                tags = listOf("IdolGlow공연장", "백스테이지", "관객프로그램"),
                summary = "무대 뒤 공간을 직접 체험하는 투어 프로그램이 관객들에게 높은 만족도를 주고 있습니다.",
                paragraphs = listOf(
                    "공연장 투어는 무대와 객석, 조명, 음향 테스트를 통해 공연 제작 과정을 입체적으로 보여줍니다.",
                    "Idol Glow는 가족 단위와 청소년 대상 프로그램을 추가해 관람 경험을 더 풍부하게 만들 예정입니다.",
                ),
            ),
            newsletter(
                slug = "idol-glow-bridges-local-commerce-with-culture-market",
                title = "Idol Glow, 지역 상권과 문화를 잇는 플리마켓 개최",
                publishedAt = LocalDate.of(2026, 3, 10),
                imageUrl = "https://images.unsplash.com/photo-1488459716781-31db52582fe9?auto=format&fit=crop&w=1200&q=80",
                tags = listOf("플리마켓", "지역상권", "문화교류", "커뮤니티"),
                summary = "지역 창작자와 방문객이 함께 만나는 플리마켓이 Idol Glow 공간에서 열렸습니다.",
                paragraphs = listOf(
                    "플리마켓은 문화소비와 로컬 상권을 연결하는 체험형 행사로 운영됐습니다.",
                    "Idol Glow는 계절형 행사와 연계한 마켓 프로그램 확대 가능성을 검토하고 있습니다.",
                ),
            ),
            newsletter(
                slug = "idol-glow-opens-media-art-special-exhibition",
                title = "Idol Glow, 2026 미디어아트 특별전 개막",
                publishedAt = LocalDate.of(2026, 2, 4),
                imageUrl = "https://images.unsplash.com/photo-1545239351-1141bd82e8a6?auto=format&fit=crop&w=1200&q=80",
                tags = listOf("미디어아트", "특별전", "전시개막"),
                summary = "몰입형 감각 환경을 주제로 한 미디어아트 특별전이 Idol Glow 전시 공간에서 개막했습니다.",
                paragraphs = listOf(
                    "이번 전시는 화면과 사운드, 인터랙티브 장치를 통해 관객의 지각 방식을 새롭게 제안합니다.",
                    "전시 기간 중 작품 해설과 연계 프로그램도 함께 운영됩니다.",
                ),
            ),
        )

        seedItems.forEach(newsletterRepository::save)
        log.info("Idol Glow 소식지 관리자 샘플 데이터를 생성했습니다.")
    }

    private fun newsletter(
        slug: String,
        title: String,
        publishedAt: LocalDate,
        imageUrl: String,
        tags: List<String>,
        summary: String,
        paragraphs: List<String>,
    ): Newsletter =
        Newsletter.create(
            slug = slug,
            draft = NewsletterDraft(
                title = title,
                categoryLabel = "Idol Glow 소식",
                publishedAt = publishedAt,
                imageUrl = imageUrl,
                summary = summary,
                tags = tags,
                paragraphs = paragraphs,
            ),
        )
}
