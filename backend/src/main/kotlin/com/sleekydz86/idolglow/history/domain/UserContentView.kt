package com.sleekydz86.idolglow.history.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.global.support.ContentTargetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDateTime

@Entity
@Table(
    name = "user_content_views",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_user_content_views_user_content",
            columnNames = ["user_id", "content_type", "content_key"],
        ),
    ],
)
class UserContentView(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    val contentType: ContentTargetType,
    @Column(name = "content_key", nullable = false, length = CONTENT_KEY_MAX_LENGTH)
    val contentKey: String,
    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    var title: String,
    @Column(nullable = false, length = 500)
    var path: String,
    @Column(name = "image_url", length = IMAGE_URL_MAX_LENGTH)
    var imageUrl: String? = null,
    @Column(name = "viewed_at", nullable = false)
    var viewedAt: LocalDateTime,
) : BaseEntity() {
    fun isOwnedBy(userId: Long): Boolean = this.userId == userId

    fun viewAgain(
        title: String,
        path: String,
        imageUrl: String?,
        now: LocalDateTime,
    ) {
        this.title = title
        this.path = path
        this.imageUrl = imageUrl
        viewedAt = now
    }

    companion object {
        const val CONTENT_KEY_MAX_LENGTH = 200
        const val TITLE_MAX_LENGTH = 300
        const val IMAGE_URL_MAX_LENGTH = 500
        const val MAX_ROWS_PER_USER = 200
    }
}
