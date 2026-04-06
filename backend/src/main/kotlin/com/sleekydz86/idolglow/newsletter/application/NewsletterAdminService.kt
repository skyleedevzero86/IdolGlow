package com.sleekydz86.idolglow.newsletter.application

import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterDetailResponse
import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterPageResponse
import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterSummaryResponse
import com.sleekydz86.idolglow.newsletter.application.dto.UpsertNewsletterCommand
import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import com.sleekydz86.idolglow.newsletter.domain.NewsletterDraft
import com.sleekydz86.idolglow.newsletter.domain.NewsletterRepository
import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionDispatchRecorder
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Transactional(readOnly = true)
@Service
class NewsletterAdminService(
    private val newsletterRepository: NewsletterRepository,
    private val adminAuditService: AdminAuditService,
    private val subscriptionDispatchRecorder: SubscriptionDispatchRecorder,
) : NewsletterAdminUseCase {

    override fun findNewsletters(page: Int, size: Int): AdminNewsletterPageResponse {
        val resolvedPage = page.coerceAtLeast(1)
        val resolvedSize = size.coerceIn(1, 20)
        val allNewsletters = newsletterRepository.findAllByLatest()

        val fromIndex = ((resolvedPage - 1) * resolvedSize).coerceAtMost(allNewsletters.size)
        val toIndex = (fromIndex + resolvedSize).coerceAtMost(allNewsletters.size)
        val totalElements = allNewsletters.size.toLong()
        val totalPages = if (totalElements == 0L) 0 else ((totalElements + resolvedSize - 1) / resolvedSize).toInt()

        return AdminNewsletterPageResponse(
            newsletters = allNewsletters.subList(fromIndex, toIndex).map(AdminNewsletterSummaryResponse::from),
            page = resolvedPage,
            size = resolvedSize,
            totalElements = totalElements,
            totalPages = totalPages,
            hasNext = toIndex < allNewsletters.size,
        )
    }

    override fun findNewsletter(newsletterSlug: String): AdminNewsletterDetailResponse =
        AdminNewsletterDetailResponse.from(getNewsletterEntity(newsletterSlug))

    @Transactional
    override fun createNewsletter(command: UpsertNewsletterCommand): AdminNewsletterDetailResponse {
        val slug = generateNewsletterSlug(command.title)
        check(!newsletterRepository.existsBySlug(slug)) {
            "동일한 제목의 소식지가 이미 등록되어 있습니다."
        }

        val savedNewsletter = newsletterRepository.save(
            Newsletter.create(
                slug = slug,
                draft = command.toDraft(),
            )
        )

        adminAuditService.log(
            actionCode = "NEWSLETTER_CREATE",
            targetType = "NEWSLETTER",
            targetId = savedNewsletter.id,
            detail = "slug=${savedNewsletter.slug}",
        )
        subscriptionDispatchRecorder.recordNewsletterDispatch(savedNewsletter)

        return AdminNewsletterDetailResponse.from(savedNewsletter)
    }

    @Transactional
    override fun updateNewsletter(
        newsletterSlug: String,
        command: UpsertNewsletterCommand,
    ): AdminNewsletterDetailResponse {
        val newsletter = getNewsletterEntity(newsletterSlug)
        val nextSlug = generateNewsletterSlug(command.title)

        check(!newsletterRepository.existsBySlugAndIdNot(nextSlug, newsletter.id)) {
            "동일한 제목의 소식지가 이미 등록되어 있습니다."
        }

        newsletter.update(
            slug = nextSlug,
            draft = command.toDraft(),
        )

        adminAuditService.log(
            actionCode = "NEWSLETTER_UPDATE",
            targetType = "NEWSLETTER",
            targetId = newsletter.id,
            detail = "slug=${newsletter.slug}",
        )

        return AdminNewsletterDetailResponse.from(newsletter)
    }

    @Transactional
    override fun deleteNewsletter(newsletterSlug: String) {
        val newsletter = getNewsletterEntity(newsletterSlug)
        val detail = "slug=${newsletter.slug}, title=${newsletter.title}"
        newsletterRepository.delete(newsletter)

        adminAuditService.log(
            actionCode = "NEWSLETTER_DELETE",
            targetType = "NEWSLETTER",
            targetId = newsletter.id,
            detail = detail,
        )
    }

    private fun getNewsletterEntity(newsletterSlug: String): Newsletter =
        newsletterRepository.findBySlug(newsletterSlug)
            ?: throw EntityNotFoundException("Newsletter not found. newsletterSlug=$newsletterSlug")

    private fun generateNewsletterSlug(title: String): String {
        val normalized = Normalizer.normalize(title, Normalizer.Form.NFKD)
            .replace(Regex("[^\\p{ASCII}]"), " ")
            .lowercase()
            .replace(Regex("[^a-z0-9\\s-]"), " ")
            .trim()
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")

        return normalized.ifBlank { "newsletter-${System.currentTimeMillis()}" }
    }

    private fun UpsertNewsletterCommand.toDraft(): NewsletterDraft {
        val normalizedTags = tags
            .flatMap { it.split(',') }
            .map { it.trim().removePrefix("#") }
            .filter { it.isNotEmpty() }
            .distinct()

        val normalizedParagraphs = paragraphs
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        return NewsletterDraft(
            title = title.trim(),
            categoryLabel = categoryLabel.trim(),
            publishedAt = parsePublishedAt(publishedAt),
            imageUrl = imageUrl.trim(),
            summary = summary.trim(),
            tags = normalizedTags,
            paragraphs = normalizedParagraphs.ifEmpty { listOf(summary.trim()) },
        )
    }

    private fun parsePublishedAt(value: String): LocalDate {
        val trimmed = value.trim().removeSuffix(".")
        require(trimmed.isNotBlank()) { "게시일은 비어 있을 수 없습니다." }

        return when {
            trimmed.matches(Regex("\\d{4}\\.\\d{2}\\.\\d{2}")) ->
                LocalDate.parse(trimmed, dottedDateFormatter)

            trimmed.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) ->
                LocalDate.parse(trimmed, dashedDateFormatter)

            else -> throw IllegalArgumentException("게시일 형식은 yyyy.MM.dd 또는 yyyy-MM-dd 이어야 합니다.")
        }
    }

    companion object {
        private val dottedDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val dashedDateFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    }
}
