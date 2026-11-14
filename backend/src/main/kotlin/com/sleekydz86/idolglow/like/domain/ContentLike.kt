package com.sleekydz86.idolglow.like.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "content_likes",
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_content_likes_user_target",
            columnNames = ["user_id", "target_type", "target_key"],
        ),
    ],
    indexes = [
        Index(name = "idx_content_likes_target", columnList = "target_type, target_key"),
    ],
)
class ContentLike(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 40)
    val targetType: LikeTargetType,
    @Column(name = "target_key", nullable = false, length = LikeTarget.KEY_MAX_LENGTH)
    val targetKey: String,
) : BaseEntity() {
    companion object {
        fun of(
            userId: Long,
            target: LikeTarget,
        ): ContentLike = ContentLike(userId = userId, targetType = target.type, targetKey = target.key)
    }
}
