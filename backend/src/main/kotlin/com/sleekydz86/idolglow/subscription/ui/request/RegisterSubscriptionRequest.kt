package com.sleekydz86.idolglow.subscription.ui.request

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterSubscriptionRequest(
    @field:NotBlank
    @field:Email
    @field:Size(max = 255)
    val email: String,

    @field:AssertTrue(message = "개인정보 수집 및 활용 동의가 필요합니다.")
    val agreedToPrivacy: Boolean,

    val subscribeNewsletters: Boolean = true,
    val subscribeIssues: Boolean = true,
)
