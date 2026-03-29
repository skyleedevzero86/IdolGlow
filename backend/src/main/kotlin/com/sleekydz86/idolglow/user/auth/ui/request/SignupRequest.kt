package com.sleekydz86.idolglow.user.auth.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "회원가입 요청 본문")
data class SignupRequest(
    @field:Schema(description = "로그인에 사용할 이메일", example = "user@example.com")
    @field:NotBlank
    val email: String,
    @field:Schema(description = "표시 닉네임", example = "idol_fan")
    @field:NotBlank
    val nickname: String,
    @field:Schema(description = "비밀번호(8~72자)", example = "securePass1!")
    @field:NotBlank
    @field:Size(min = 8, max = 72)
    val password: String,
)
