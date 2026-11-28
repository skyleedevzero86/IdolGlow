package com.sleekydz86.idolglow.user.auth.domain

import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.domain.vo.OAuthAccountDetails
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

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
    @Column(name = "account_name", length = 100)
    var accountName: String? = null,
    @Column(name = "gender", length = 10)
    var gender: String? = null,
    @Column(name = "age_range", length = 20)
    var ageRange: String? = null,
) {
    fun updateProfile(
        name: String?,
        picture: String?,
    ) {
        profileName = name?.trim()?.takeIf { it.isNotEmpty() }
        profileImageUrl = picture?.trim()?.takeIf { it.isNotEmpty() }
    }

    fun updateAccountDetails(details: OAuthAccountDetails) {
        accountName = normalized(details.accountName, ACCOUNT_NAME_MAX)
        gender = normalized(details.gender, GENDER_MAX)
        ageRange = normalized(details.ageRange, AGE_RANGE_MAX)
    }

    private fun normalized(
        value: String?,
        maxCodePoints: Int,
    ): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val fits = trimmed.codePointCount(0, trimmed.length) <= maxCodePoints
        return if (fits) trimmed else trimmed.substring(0, trimmed.offsetByCodePoints(0, maxCodePoints))
    }

    companion object {
        private const val ACCOUNT_NAME_MAX = 100
        private const val GENDER_MAX = 10
        private const val AGE_RANGE_MAX = 20

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
