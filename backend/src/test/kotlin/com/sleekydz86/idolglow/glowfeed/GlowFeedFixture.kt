package com.sleekydz86.idolglow.glowfeed

import com.sleekydz86.idolglow.comment.domain.ContentComment
import com.sleekydz86.idolglow.comment.infrastructure.ContentCommentJpaRepository
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.global.support.ContentTargetType
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import com.sleekydz86.idolglow.glowcommunity.infrastructure.GlowCommunityPostJpaRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductJpaRepository
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.webzine.domain.IssueCategory
import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSectionDraft
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import com.sleekydz86.idolglow.webzine.domain.WebzineIssueRepository
import org.springframework.context.ApplicationContext
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID
import kotlin.random.Random

class GlowFeedFixture(
    private val context: ApplicationContext,
) {
    val token: String = "gf" + randomSuffix()

    private val jdbc: JdbcTemplate by lazy { context.getBean(JdbcTemplate::class.java) }
    private val transactions: TransactionTemplate by lazy { context.getBean(TransactionTemplate::class.java) }
    private val issues: WebzineIssueRepository by lazy { context.getBean(WebzineIssueRepository::class.java) }
    private val cleanups = mutableListOf<Pair<String, Long>>()
    private val userIds = mutableListOf<Long>()
    private val issueSlugs = mutableListOf<String>()
    private var fakeUserId = FAKE_USER_BASE + Random.nextLong(FAKE_USER_RANGE) * FAKE_USER_STEP

    fun member(): User {
        val suffix = randomSuffix()
        val user =
            context
                .getBean(UserJpaRepository::class.java)
                .save(User(email = "gf$suffix@test.com", nickname = Nickname.of("g$suffix")))
        userIds += user.id
        return user
    }

    fun article(
        issueDate: LocalDate,
        title: String,
    ): FeedArticle {
        val issueSlug = "$token-${issueSlugs.size}"
        val volume = VOLUME_BASE + Random.nextInt(VOLUME_RANGE)
        issueSlugs += issueSlug
        val article =
            transactions.execute {
                val issue = WebzineIssue.create(issueSlug, volume, issueDate, "https://example.com/cover.jpg", "소개")
                issue.addArticle(WebzineArticle.create(issue, "$issueSlug-a", draft(title)))
                issues.save(issue).articles.first()
            }!!
        return FeedArticle(article.id, issueSlug, "$issueSlug-a", volume)
    }

    fun event(endDate: LocalDate): EventSourceRef {
        val ref =
            EventSourceRef.create("TOUR_API", "$token-${Random.nextInt(EVENT_ID_RANGE)}").apply {
                title = "$token 행사"
                startDate = endDate.minusDays(EVENT_DAYS)
                this.endDate = endDate
                venue = "세종문화회관"
            }
        val saved = context.getBean(EventSourceRefJpaRepository::class.java).save(ref)
        cleanups += "DELETE FROM event_source_ref WHERE id = ?" to saved.id
        return saved
    }

    fun review(
        author: User,
        hidden: Boolean,
    ): ProductReview {
        val product =
            context
                .getBean(ProductJpaRepository::class.java)
                .save(Product(name = "$token 상품", description = "설명"))
        cleanups += "DELETE FROM products WHERE id = ?" to product.id
        val review = ProductReview.of(product, author.id, RATING, "$token 리뷰 본문")
        if (hidden) review.hide(LocalDateTime.now(), "신고")
        val saved = context.getBean(ProductReviewJpaRepository::class.java).save(review)
        cleanups.add(cleanups.lastIndex, "DELETE FROM product_reviews WHERE id = ?" to saved.id)
        return saved
    }

    fun comment(
        author: User,
        targetKey: String,
        deleted: Boolean,
    ): ContentComment {
        val comment =
            ContentComment.create(
                userId = author.id,
                targetType = ContentTargetType.PRODUCT,
                targetKey = targetKey,
                targetTitle = "$token 상품",
                targetPath = "/products/$targetKey",
                contentMarkdown = "$token 댓글",
            )
        if (deleted) comment.delete(LocalDateTime.now())
        val saved = context.getBean(ContentCommentJpaRepository::class.java).save(comment)
        cleanups += "DELETE FROM content_comments WHERE id = ?" to saved.id
        return saved
    }

    fun post(
        author: User,
        blinded: Boolean,
    ): GlowCommunityPost {
        val post = GlowCommunityPost.create(author.id, GlowCommunityTag.TALK, "$token 글", "본문")
        if (blinded) post.blind(LocalDateTime.now(), "관리자")
        val saved = context.getBean(GlowCommunityPostJpaRepository::class.java).save(post)
        cleanups += "DELETE FROM glow_community_posts WHERE id = ?" to saved.id
        return saved
    }

    fun createdAt(
        table: String,
        id: Long,
        at: LocalDateTime,
    ) {
        jdbc.update("UPDATE $table SET created_at = ? WHERE id = ?", Timestamp.valueOf(at), id)
    }

    fun likePost(
        postId: Long,
        likeCount: Long,
    ) {
        jdbc.update("UPDATE glow_community_posts SET like_count = ? WHERE id = ?", likeCount, postId)
    }

    fun react(
        likeType: String?,
        viewType: String?,
        key: String,
        count: Int,
    ) {
        repeat(count) {
            val userId = fakeUserId++
            userIds += userId
            val now = Timestamp.valueOf(LocalDateTime.now())
            likeType?.let { jdbc.update(LIKE_SQL, userId, it, key, now, now) }
            viewType?.let { jdbc.update(VIEW_SQL, userId, it, key, "/glow", now, now, now) }
        }
    }

    fun cleanup() {
        userIds.forEach { userId ->
            USER_REACTION_TABLES.forEach { table -> jdbc.update("DELETE FROM $table WHERE user_id = ?", userId) }
        }
        cleanups.forEach { (sql, id) -> jdbc.update(sql, id) }
        cleanups.clear()
        transactions.executeWithoutResult {
            val repository = issues
            issueSlugs.mapNotNull(repository::findBySlug).forEach(repository::delete)
        }
    }

    data class FeedArticle(
        val id: Long,
        val issueSlug: String,
        val articleSlug: String,
        val volume: Int,
    ) {
        val key: String
            get() = "$issueSlug/$articleSlug"
    }

    companion object {
        private const val SUFFIX_LENGTH = 8
        private const val VOLUME_BASE = 700_000
        private const val VOLUME_RANGE = 200_000
        private const val FAKE_USER_BASE = 900_000_000L
        private const val FAKE_USER_RANGE = 100_000L
        private const val FAKE_USER_STEP = 100L
        private const val EVENT_ID_RANGE = 1_000_000
        private const val EVENT_DAYS = 3L
        private const val RATING = 5
        private val USER_REACTION_TABLES = listOf("content_likes", "user_content_views", "glow_community_likes")
        private const val LIKE_SQL =
            "INSERT INTO content_likes (user_id, target_type, target_key, created_at, updated_at) VALUES (?, ?, ?, ?, ?)"
        private const val VIEW_SQL =
            "INSERT INTO user_content_views (user_id, content_type, content_key, title, path, viewed_at, " +
                "created_at, updated_at) VALUES (?, ?, ?, 't', ?, ?, ?, ?)"

        private fun randomSuffix(): String =
            UUID
                .randomUUID()
                .toString()
                .replace("-", "")
                .take(SUFFIX_LENGTH)

        private fun draft(title: String): WebzineArticleDraft =
            WebzineArticleDraft(
                title = title,
                kicker = "부제",
                summary = "요약",
                heroImageUrl = "https://example.com/hero.jpg",
                cardImageUrl = "https://example.com/card.jpg",
                category = IssueCategory.EXHIBITION,
                formatLabel = "아티클",
                authorName = "기자",
                authorEmail = "writer@example.com",
                creditLine = "크레딧",
                highlightQuote = null,
                sections = listOf(WebzineArticleSectionDraft(heading = "소제목", body = "본문", note = null)),
                galleryImageUrls = emptyList(),
                tags = listOf("트렌드"),
            )
    }
}
