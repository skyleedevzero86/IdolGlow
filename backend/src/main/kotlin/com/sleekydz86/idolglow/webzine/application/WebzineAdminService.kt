package com.sleekydz86.idolglow.webzine.application

import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueArticleResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssuePageResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueRelatedContentResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueSummaryResponse
import com.sleekydz86.idolglow.webzine.application.dto.AdminIssueVolumeResponse
import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSectionDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import com.sleekydz86.idolglow.webzine.infrastructure.WebzineArticleJpaRepository
import com.sleekydz86.idolglow.webzine.infrastructure.WebzineIssueJpaRepository
import com.sleekydz86.idolglow.webzine.ui.request.CreateWebzineIssueRequest
import com.sleekydz86.idolglow.webzine.ui.request.UpsertWebzineArticleRequest
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class WebzineAdminService(
    private val webzineIssueJpaRepository: WebzineIssueJpaRepository,
    private val webzineArticleJpaRepository: WebzineArticleJpaRepository,
    private val adminAuditService: AdminAuditService,
) {

    fun findIssues(
        page: Int,
        size: Int,
        year: Int?,
        month: Int?,
        volume: Int?,
    ): AdminIssuePageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 20)
        val filteredIssues = webzineIssueJpaRepository.findAll(
            Sort.by(
                Sort.Order.desc("volume"),
                Sort.Order.desc("issueDate"),
            )
        ).filter { issue ->
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
            latestVolume = webzineIssueJpaRepository.findTopByOrderByVolumeDesc()?.volume ?: 0,
            totalArticleCount = filteredIssues.sumOf { it.articles.size },
            availableYears = filteredIssues.map { it.issueDate.year }.distinct().sortedDescending(),
            availableMonths = filteredIssues.map { it.issueDate.monthValue }.distinct().sorted(),
            availableVolumes = filteredIssues.map { it.volume }.distinct().sortedDescending(),
        )
    }

    fun findIssue(issueSlug: String): AdminIssueVolumeResponse =
        AdminIssueVolumeResponse.from(getIssueEntity(issueSlug))

    fun findArticle(issueSlug: String, articleSlug: String): AdminIssueArticleResponse {
        val issue = getIssueEntity(issueSlug)
        val article = getArticleEntity(issue, articleSlug)
        return AdminIssueArticleResponse.from(article, buildRelatedContents(issue, article))
    }

    @Transactional
    fun createIssue(request: CreateWebzineIssueRequest): AdminIssueVolumeResponse {
        val issueSlug = "vol-${request.volume}"
        require(!webzineIssueJpaRepository.existsByVolumeOrSlug(request.volume, issueSlug)) {
            "Vol.${request.volume} 는 이미 등록되어 있습니다."
        }

        val savedIssue = webzineIssueJpaRepository.save(
            WebzineIssue.create(
                slug = issueSlug,
                volume = request.volume,
                issueDate = parseIssueDate(request.issueDate),
                coverImageUrl = request.coverImageUrl,
                teaser = request.teaser,
            )
        )

        adminAuditService.log(
            actionCode = "WEBZINE_ISSUE_CREATE",
            targetType = "WEBZINE_ISSUE",
            targetId = savedIssue.id,
            detail = "slug=${savedIssue.slug}",
        )

        return AdminIssueVolumeResponse.from(savedIssue)
    }

    @Transactional
    fun updateIssue(issueSlug: String, request: CreateWebzineIssueRequest): AdminIssueVolumeResponse {
        val issue = getIssueEntity(issueSlug)
        val nextSlug = "vol-${request.volume}"

        require(!webzineIssueJpaRepository.existsByVolumeAndIdNot(request.volume, issue.id)) {
            "Vol.${request.volume} ???대? ?깅줉?섏뼱 ?덉뒿?덈떎."
        }
        require(!webzineIssueJpaRepository.existsBySlugAndIdNot(nextSlug, issue.id)) {
            "Vol.${request.volume} ?ㅻ윭洹몃? ?대? ?ъ슜 以묒엯?덈떎."
        }

        issue.slug = nextSlug
        issue.volume = request.volume
        issue.update(
            issueDate = parseIssueDate(request.issueDate),
            coverImageUrl = request.coverImageUrl,
            teaser = request.teaser,
        )

        adminAuditService.log(
            actionCode = "WEBZINE_ISSUE_UPDATE",
            targetType = "WEBZINE_ISSUE",
            targetId = issue.id,
            detail = "slug=${issue.slug}",
        )

        return AdminIssueVolumeResponse.from(issue)
    }

    @Transactional
    fun deleteIssue(issueSlug: String) {
        val issue = getIssueEntity(issueSlug)
        val issueId = issue.id
        val detail = "slug=${issue.slug}, volume=${issue.volume}"
        webzineIssueJpaRepository.delete(issue)

        adminAuditService.log(
            actionCode = "WEBZINE_ISSUE_DELETE",
            targetType = "WEBZINE_ISSUE",
            targetId = issueId,
            detail = detail,
        )
    }

    @Transactional
    fun createArticle(issueSlug: String, request: UpsertWebzineArticleRequest): AdminIssueArticleResponse {
        val issue = getIssueEntity(issueSlug)
        val article = WebzineArticle.create(
            issue = issue,
            slug = generateArticleSlug(issue, request.title),
            draft = request.toDraft(issue),
        )

        issue.addArticle(article)
        val savedArticle = webzineArticleJpaRepository.save(article)

        adminAuditService.log(
            actionCode = "WEBZINE_ARTICLE_CREATE",
            targetType = "WEBZINE_ARTICLE",
            targetId = savedArticle.id,
            detail = "issueSlug=${issue.slug}, articleSlug=${savedArticle.slug}",
        )

        return AdminIssueArticleResponse.from(savedArticle, buildRelatedContents(issue, savedArticle))
    }

    @Transactional
    fun updateArticle(
        issueSlug: String,
        articleSlug: String,
        request: UpsertWebzineArticleRequest,
    ): AdminIssueArticleResponse {
        val issue = getIssueEntity(issueSlug)
        val article = getArticleEntity(issue, articleSlug)
        val nextSlug = generateArticleSlug(issue, request.title, article.id)

        article.update(
            slug = nextSlug,
            draft = request.toDraft(issue),
        )

        adminAuditService.log(
            actionCode = "WEBZINE_ARTICLE_UPDATE",
            targetType = "WEBZINE_ARTICLE",
            targetId = article.id,
            detail = "issueSlug=${issue.slug}, articleSlug=${article.slug}",
        )

        return AdminIssueArticleResponse.from(article, buildRelatedContents(issue, article))
    }

    @Transactional
    fun deleteArticle(issueSlug: String, articleSlug: String) {
        val issue = getIssueEntity(issueSlug)
        val article = getArticleEntity(issue, articleSlug)
        webzineArticleJpaRepository.delete(article)

        adminAuditService.log(
            actionCode = "WEBZINE_ARTICLE_DELETE",
            targetType = "WEBZINE_ARTICLE",
            targetId = article.id,
            detail = "issueSlug=${issue.slug}, articleSlug=${article.slug}",
        )
    }

    private fun getIssueEntity(issueSlug: String): WebzineIssue =
        webzineIssueJpaRepository.findBySlug(issueSlug)
            ?: throw EntityNotFoundException("웹진 호를 찾을 수 없습니다. issueSlug=$issueSlug")

    private fun getArticleEntity(issue: WebzineIssue, articleSlug: String): WebzineArticle =
        webzineArticleJpaRepository.findByIssue_IdAndSlug(issue.id, articleSlug)
            ?: throw EntityNotFoundException(
                "웹진 기사를 찾을 수 없습니다. issueSlug=${issue.slug}, articleSlug=$articleSlug"
            )

    private fun buildRelatedContents(
        issue: WebzineIssue,
        currentArticle: WebzineArticle,
    ): List<AdminIssueRelatedContentResponse> =
        issue.articles
            .filter { it.id != currentArticle.id }
            .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
            .take(6)
            .map(AdminIssueRelatedContentResponse::from)

    private fun generateArticleSlug(
        issue: WebzineIssue,
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
        require(trimmed.isNotEmpty()) { "발행일은 비어 있을 수 없습니다." }

        val normalized = trimmed.removeSuffix(".")

        return when {
            normalized.matches(Regex("\\d{4}\\.\\d{2}")) ->
                YearMonth.parse(normalized, dottedYearMonthFormatter).atDay(1)

            normalized.matches(Regex("\\d{4}-\\d{2}")) ->
                YearMonth.parse(normalized, dashedYearMonthFormatter).atDay(1)

            normalized.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ->
                LocalDate.parse(normalized, dashedDateFormatter)

            else -> throw IllegalArgumentException("발행일 형식은 2026.03. 또는 2026-03 이어야 합니다.")
        }
    }

    private fun slugify(value: String): String {
        val normalized = Normalizer.normalize(value, Normalizer.Form.NFKD)
            .replace(Regex("[^\\p{ASCII}]"), " ")
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), " ")
            .trim()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")

        return normalized.ifBlank { "article-${System.currentTimeMillis()}" }
    }

    private fun UpsertWebzineArticleRequest.toDraft(issue: WebzineIssue): WebzineArticleDraft {
        val normalizedSections = sections
            .map {
                WebzineArticleSectionDraft(
                    heading = it.heading?.trim()?.takeIf { heading -> heading.isNotEmpty() },
                    body = it.body.trim(),
                    note = it.note?.trim()?.takeIf { note -> note.isNotEmpty() },
                )
            }
            .ifEmpty {
                listOf(
                    WebzineArticleSectionDraft(
                        heading = null,
                        body = "본문 내용을 입력해 주세요.",
                        note = null,
                    )
                )
            }

        val normalizedGallery = buildList {
            add(heroImageUrl.trim())
            add(cardImageUrl.trim())
            addAll(galleryImageUrls.map { it.trim() }.filter { it.isNotEmpty() })
            add(issue.coverImageUrl)
        }.distinct()

        val normalizedTags = tags
            .flatMap { it.split(',') }
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinct()

        return WebzineArticleDraft(
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
