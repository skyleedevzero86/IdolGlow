package com.sleekydz86.idolglow.idolglowissue.application

import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssueArticleResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssuePageResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssueRelatedContentResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssueSummaryResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.AdminIssueVolumeResponse
import com.sleekydz86.idolglow.idolglowissue.application.dto.CreateIdolGlowIssueCommand
import com.sleekydz86.idolglow.idolglowissue.application.dto.UpsertIdolGlowArticleCommand
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticle
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleDraft
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleRepository
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleSectionDraft
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssue
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowIssueRepository
import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class IdolGlowAdminService(
    private val idolglowIssueRepository: IdolGlowIssueRepository,
    private val idolglowArticleRepository: IdolGlowArticleRepository,
    private val adminAuditService: AdminAuditService,
) : IdolGlowAdminUseCase {
    override fun findIssues(
        page: Int,
        size: Int,
        year: Int?,
        month: Int?,
        volume: Int?,
    ): AdminIssuePageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 20)
        val allIssues = idolglowIssueRepository.findAllByLatest()
        val filteredIssues =
            allIssues.filter { issue ->
                (year == null || issue.issueDate.year == year) &&
                    (month == null || issue.issueDate.monthValue == month) &&
                    (volume == null || issue.volume == volume)
            }

        val fromIndex = ((resolvedPage - 1) * resolvedSize).coerceAtMost(filteredIssues.size)
        val toIndex = (fromIndex + resolvedSize).coerceAtMost(filteredIssues.size)
        val totalElements = filteredIssues.size.toLong()
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + resolvedSize - 1) / resolvedSize).toInt()

        return AdminIssuePageResponse(
            issues = filteredIssues.subList(fromIndex, toIndex).map(AdminIssueSummaryResponse::from),
            page = resolvedPage,
            size = resolvedSize,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = toIndex < filteredIssues.size,
            latestVolume = idolglowIssueRepository.findTopByLatestVolume()?.volume ?: 0,
            totalArticleCount = filteredIssues.sumOf { it.articles.size },
            availableYears = allIssues.map { it.issueDate.year }.distinct().sortedDescending(),
            availableMonths = allIssues.map { it.issueDate.monthValue }.distinct().sorted(),
            availableVolumes = allIssues.map { it.volume }.distinct().sortedDescending(),
        )
    }

    override fun findIssue(issueSlug: String): AdminIssueVolumeResponse = AdminIssueVolumeResponse.from(getIssueEntity(issueSlug))

    override fun findArticle(
        issueSlug: String,
        articleSlug: String,
    ): AdminIssueArticleResponse {
        val issue = getIssueEntity(issueSlug)
        val article = getArticleEntity(issue, articleSlug)
        return AdminIssueArticleResponse.from(article, buildRelatedContents(issue, article))
    }

    @Transactional
    override fun createIssue(command: CreateIdolGlowIssueCommand): AdminIssueVolumeResponse {
        val issueSlug = "vol-${command.volume}"
        require(!idolglowIssueRepository.existsByVolumeOrSlug(command.volume, issueSlug)) {
            "호 번호 ${command.volume}은(는) 이미 등록되어 있습니다."
        }

        val savedIssue =
            idolglowIssueRepository.save(
                IdolGlowIssue.create(
                    slug = issueSlug,
                    volume = command.volume,
                    issueDate = parseIssueDate(command.issueDate),
                    coverImageUrl = command.coverImageUrl,
                    teaser = command.teaser,
                ),
            )

        adminAuditService.log(
            actionCode = "IDOLGLOW_ISSUE_CREATE",
            targetType = "IDOLGLOW_ISSUE",
            targetId = savedIssue.id,
            detail = "slug=${savedIssue.slug}",
        )

        return AdminIssueVolumeResponse.from(savedIssue)
    }

    @Transactional
    override fun updateIssue(
        issueSlug: String,
        command: CreateIdolGlowIssueCommand,
    ): AdminIssueVolumeResponse {
        val issue = getIssueEntity(issueSlug)
        val nextSlug = "vol-${command.volume}"

        require(!idolglowIssueRepository.existsByVolumeAndIdNot(command.volume, issue.id)) {
            "호 번호 ${command.volume}은(는) 다른 호에서 이미 사용 중입니다."
        }
        require(!idolglowIssueRepository.existsBySlugAndIdNot(nextSlug, issue.id)) {
            "슬러그 $nextSlug 은(는) 다른 호에서 이미 사용 중입니다."
        }

        issue.slug = nextSlug
        issue.volume = command.volume
        issue.update(
            issueDate = parseIssueDate(command.issueDate),
            coverImageUrl = command.coverImageUrl,
            teaser = command.teaser,
        )

        adminAuditService.log(
            actionCode = "IDOLGLOW_ISSUE_UPDATE",
            targetType = "IDOLGLOW_ISSUE",
            targetId = issue.id,
            detail = "slug=${issue.slug}",
        )

        return AdminIssueVolumeResponse.from(issue)
    }

    @Transactional
    override fun deleteIssue(issueSlug: String) {
        val issue = getIssueEntity(issueSlug)
        val issueId = issue.id
        val detail = "slug=${issue.slug}, volume=${issue.volume}"
        idolglowIssueRepository.delete(issue)

        adminAuditService.log(
            actionCode = "IDOLGLOW_ISSUE_DELETE",
            targetType = "IDOLGLOW_ISSUE",
            targetId = issueId,
            detail = detail,
        )
    }

    @Transactional
    override fun createArticle(
        issueSlug: String,
        command: UpsertIdolGlowArticleCommand,
    ): AdminIssueArticleResponse {
        val issue = getIssueEntity(issueSlug)
        val article =
            IdolGlowArticle.create(
                issue = issue,
                slug = generateArticleSlug(issue, command.title),
                draft = command.toDraft(issue),
            )

        issue.addArticle(article)
        val savedArticle = idolglowArticleRepository.save(article)

        adminAuditService.log(
            actionCode = "IDOLGLOW_ARTICLE_CREATE",
            targetType = "IDOLGLOW_ARTICLE",
            targetId = savedArticle.id,
            detail = "issueSlug=${issue.slug}, articleSlug=${savedArticle.slug}",
        )

        return AdminIssueArticleResponse.from(savedArticle, buildRelatedContents(issue, savedArticle))
    }

    @Transactional
    override fun updateArticle(
        issueSlug: String,
        articleSlug: String,
        command: UpsertIdolGlowArticleCommand,
    ): AdminIssueArticleResponse {
        val issue = getIssueEntity(issueSlug)
        val article = getArticleEntity(issue, articleSlug)
        val nextSlug = generateArticleSlug(issue, command.title, article.id)

        article.update(
            slug = nextSlug,
            draft = command.toDraft(issue),
        )

        adminAuditService.log(
            actionCode = "IDOLGLOW_ARTICLE_UPDATE",
            targetType = "IDOLGLOW_ARTICLE",
            targetId = article.id,
            detail = "issueSlug=${issue.slug}, articleSlug=${article.slug}",
        )

        return AdminIssueArticleResponse.from(article, buildRelatedContents(issue, article))
    }

    @Transactional
    override fun deleteArticle(
        issueSlug: String,
        articleSlug: String,
    ) {
        val issue = getIssueEntity(issueSlug)
        val article = getArticleEntity(issue, articleSlug)
        idolglowArticleRepository.delete(article)

        adminAuditService.log(
            actionCode = "IDOLGLOW_ARTICLE_DELETE",
            targetType = "IDOLGLOW_ARTICLE",
            targetId = article.id,
            detail = "issueSlug=${issue.slug}, articleSlug=${article.slug}",
        )
    }

    private fun getIssueEntity(issueSlug: String): IdolGlowIssue =
        idolglowIssueRepository.findBySlug(issueSlug)
            ?: throw EntityNotFoundException("웹진 호를 찾을 수 없습니다. issueSlug=$issueSlug")

    private fun getArticleEntity(
        issue: IdolGlowIssue,
        articleSlug: String,
    ): IdolGlowArticle =
        idolglowArticleRepository.findByIssueIdAndSlug(issue.id, articleSlug)
            ?: throw EntityNotFoundException(
                "웹진 기사를 찾을 수 없습니다. issueSlug=${issue.slug}, articleSlug=$articleSlug",
            )

    private fun buildRelatedContents(
        issue: IdolGlowIssue,
        currentArticle: IdolGlowArticle,
    ): List<AdminIssueRelatedContentResponse> =
        issue.articles
            .filter { it.id != currentArticle.id }
            .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
            .take(6)
            .map(AdminIssueRelatedContentResponse::from)

    private fun generateArticleSlug(
        issue: IdolGlowIssue,
        title: String,
        currentArticleId: Long? = null,
    ): String {
        val baseSlug = slugify(title)
        var candidate = baseSlug
        var suffix = 2

        while (issue.articles.any { it.slug == candidate && it.id != currentArticleId }) {
            candidate = "$baseSlug-$suffix"
            suffix += 1
        }

        return candidate
    }

    private fun parseIssueDate(value: String): LocalDate {
        val trimmed = value.trim()
        require(trimmed.isNotEmpty()) { "호 발행일은 비울 수 없습니다." }

        val normalized = trimmed.removeSuffix(".")

        return when {
            normalized.matches(Regex("\\d{4}\\.\\d{2}")) ->
                YearMonth.parse(normalized, dottedYearMonthFormatter).atDay(1)

            normalized.matches(Regex("\\d{4}-\\d{2}")) ->
                YearMonth.parse(normalized, dashedYearMonthFormatter).atDay(1)

            normalized.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ->
                LocalDate.parse(normalized, dashedDateFormatter)

            else -> throw IllegalArgumentException("호 발행일 형식은 yyyy.MM, yyyy-MM, yyyy-MM-dd 중 하나여야 합니다.")
        }
    }

    private fun slugify(value: String): String {
        val normalized =
            Normalizer
                .normalize(value, Normalizer.Form.NFKD)
                .replace(Regex("[^\\p{ASCII}]"), " ")
                .lowercase()
                .replace(Regex("[^a-z0-9\\s-]"), " ")
                .trim()
                .replace(Regex("\\s+"), "-")
                .replace(Regex("-+"), "-")

        return normalized.ifBlank { "article-${System.currentTimeMillis()}" }
    }

    private fun UpsertIdolGlowArticleCommand.toDraft(issue: IdolGlowIssue): IdolGlowArticleDraft {
        val normalizedSections =
            sections
                .map {
                    IdolGlowArticleSectionDraft(
                        heading = it.heading?.trim()?.takeIf { heading -> heading.isNotEmpty() },
                        body = it.body.trim(),
                        note = it.note?.trim()?.takeIf { note -> note.isNotEmpty() },
                    )
                }.ifEmpty {
                    listOf(
                        IdolGlowArticleSectionDraft(
                            heading = null,
                            body = "기사 본문을 입력해 주세요.",
                            note = null,
                        ),
                    )
                }

        val normalizedGallery =
            buildList {
                add(heroImageUrl.trim())
                add(cardImageUrl.trim())
                addAll(galleryImageUrls.map { it.trim() }.filter { it.isNotEmpty() })
                add(issue.coverImageUrl)
            }.distinct()

        val normalizedTags =
            tags
                .flatMap { it.split(',') }
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotEmpty() }
                .distinct()

        return IdolGlowArticleDraft(
            title = title.trim(),
            kicker = kicker.trim(),
            summary = summary.trim(),
            heroImageUrl = heroImageUrl.trim(),
            cardImageUrl = cardImageUrl.trim(),
            category = category,
            formatLabel = formatLabel.trim(),
            authorName = authorName.trim(),
            authorEmail = authorEmail.trim(),
            creditLine = creditLine.trim(),
            highlightQuote = highlightQuote?.trim()?.takeIf { quote -> quote.isNotEmpty() },
            sections = normalizedSections,
            galleryImageUrls = normalizedGallery,
            tags = normalizedTags,
        )
    }

    companion object {
        private val dottedYearMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM")
        private val dashedYearMonthFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        private val dashedDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
