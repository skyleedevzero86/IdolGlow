package com.sleekydz86.idolglow.user.auth.graphql

import com.sleekydz86.idolglow.user.auth.application.AuthService
import com.sleekydz86.idolglow.user.auth.application.SignupService
import com.sleekydz86.idolglow.user.auth.ui.request.SignupRequest
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class AuthGraphQlController(
    private val signupService: SignupService,
    private val authService: AuthService,
) {

    @QueryMapping
    fun checkSignupEmail(@Argument email: String?): SignupCheckGraphQlResponse =
        SignupCheckGraphQlResponse.from(signupService.checkEmailField(email.orEmpty()))

    @QueryMapping
    fun checkSignupNickname(@Argument nickname: String?): SignupCheckGraphQlResponse =
        SignupCheckGraphQlResponse.from(signupService.checkNicknameField(nickname.orEmpty()))

    @MutationMapping
    fun signup(@Argument @Valid input: SignupRequest): AuthTokenGraphQlResponse =
        AuthTokenGraphQlResponse.from(
            signupService.signup(
                email = input.email,
                rawNickname = input.nickname,
                password = input.password
            )
        )

    @MutationMapping
    fun reissueToken(@Argument refreshToken: String): AuthTokenGraphQlResponse =
        AuthTokenGraphQlResponse.from(authService.reissue(refreshToken))
}
