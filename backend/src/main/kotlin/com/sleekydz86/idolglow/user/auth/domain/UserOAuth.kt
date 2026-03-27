package com.sleekydz86.idolglow.user.auth.domain

import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import jakarta.persistence.*

@Entity
@Table(name = "user_oauths")
class UserOAuth(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long = 0,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var provider: AuthProvider,

    @Column(name = "provider_id", nullable = false)
    val providerId: String,

    @Column(nullable = false)
    val email: String,

    @Column(name = "profile_name", length = 255)
    var profileName: String? = null,

    @Column(name = "profile_image_url", length = 500)
    var profileImageUrl: String? = null,
) {
    fun updateProfile(name: String?, picture: String?) {
        profileName = name?.trim()?.takeIf { it.isNotEmpty() }
        profileImageUrl = picture?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        fun of(
            userId: Long,
            provider: AuthProvider,
            providerId: String,
            email: String,
            profileName: String? = null,
            profileImageUrl: String? = null,
        ): UserOAuth =
            UserOAuth(
                userId = userId,
                provider = provider,
                providerId = providerId,
                email = email,
                profileName = profileName?.trim()?.takeIf { it.isNotEmpty() },
                profileImageUrl = profileImageUrl?.trim()?.takeIf { it.isNotEmpty() },
            )
    }
}
