package com.sleekydz86.idolglow.glowcommunity.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "glow_community_likes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_glow_community_likes_post_user", columnNames = ["post_id", "user_id"]),
    ],
)
class GlowCommunityLike(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "post_id", nullable = false)
    val postId: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : BaseEntity()
