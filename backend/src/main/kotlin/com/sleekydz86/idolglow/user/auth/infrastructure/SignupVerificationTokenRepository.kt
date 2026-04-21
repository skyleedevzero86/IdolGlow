package com.sleekydz86.idolglow.user.auth.infrastructure

import com.sleekydz86.idolglow.user.auth.domain.SignupVerificationStatus
import com.sleekydz86.idolglow.user.auth.domain.SignupVerificationToken
import com.sleekydz86.idolglow.user.auth.domain.SignupVerificationType
import org.springframework.data.jpa.repository.JpaRepository

interface SignupVerificationTokenRepository : JpaRepository<SignupVerificationToken, Long> {
    fun findByToken(token: String): SignupVerificationToken?

    fun findTopByEmailAndTypeAndStatusOrderByCreatedAtDesc(
        email: String,
        type: SignupVerificationType,
        status: SignupVerificationStatus,
    ): SignupVerificationToken?
}
