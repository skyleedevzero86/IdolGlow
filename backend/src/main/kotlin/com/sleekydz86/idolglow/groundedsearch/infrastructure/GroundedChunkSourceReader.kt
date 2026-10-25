package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.application.port.GroundedSourcePort
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChunkDraft
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChunkText
import com.sleekydz86.idolglow.groundedsearch.domain.GroundedLinkKey
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class GroundedChunkSourceReader(
    private val entityManager: EntityManager,
) : GroundedSourcePort {
    @Transactional(readOnly = true)
    override fun readAll(): List<GroundedChunkDraft> = productDrafts() + optionDrafts() + articleDrafts()

    private fun productDrafts(): List<GroundedChunkDraft> =
        products().mapNotNull { product ->
            draft(
                RagSourceType.PRODUCT,
                product.id,
                GroundedLinkKey.product(product.id),
                GroundedChunkText.fromProduct(product),
            )
        }

    private fun optionDrafts(): List<GroundedChunkDraft> =
        options().mapNotNull { option ->
            draft(
                RagSourceType.OPTION,
                option.id,
                GroundedLinkKey.option(option.id),
                GroundedChunkText.fromOption(option),
            )
        }

    private fun articleDrafts(): List<GroundedChunkDraft> = articles().flatMap { article -> draftsOf(article) }

    private fun draftsOf(article: WebzineArticle): List<GroundedChunkDraft> {
        val issueSlug = article.issue.slug
        val articleDraft =
            draft(
                RagSourceType.WEBZINE_ARTICLE,
                article.id,
                GroundedLinkKey.article(issueSlug, article.slug),
                GroundedChunkText.fromArticle(article),
            )
        val sections =
            article.sections.mapNotNull { section ->
                draft(
                    RagSourceType.WEBZINE_SECTION,
                    section.id,
                    GroundedLinkKey.section(issueSlug, article.slug, section.id),
                    GroundedChunkText.fromSection(section),
                )
            }
        return listOfNotNull(articleDraft) + sections
    }

    private fun draft(
        sourceType: RagSourceType,
        sourceId: Long,
        linkKey: String,
        content: String,
    ): GroundedChunkDraft? {
        if (sourceId == 0L || content.isBlank()) {
            return null
        }
        return GroundedChunkDraft(sourceType, sourceId, linkKey, content)
    }

    private fun products(): List<Product> =
        entityManager.createQuery("select p from Product p", Product::class.java).resultList

    private fun options(): List<Option> =
        entityManager.createQuery("select o from Option o", Option::class.java).resultList

    private fun articles(): List<WebzineArticle> =
        entityManager
            .createQuery("select distinct a from WebzineArticle a join fetch a.issue", WebzineArticle::class.java)
            .resultList
}
