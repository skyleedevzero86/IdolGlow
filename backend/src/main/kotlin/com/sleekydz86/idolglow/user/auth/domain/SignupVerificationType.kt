package com.sleekydz86.idolglow.user.auth.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

enum class SignupVerificationType {
    EMAIL_SIGNUP_VERIFY,
    ACCOUNT_CONFIRM,
}
