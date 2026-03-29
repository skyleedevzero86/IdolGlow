package com.sleekydz86.idolglow.user.user.graphql

import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.user.user.application.UserService
import com.sleekydz86.idolglow.user.user.application.UserSurveyCommandService
import com.sleekydz86.idolglow.user.user.application.UserSurveyQueryService
import com.sleekydz86.idolglow.user.user.ui.request.CreateUserSurveyRequest
import com.sleekydz86.idolglow.user.user.ui.request.UpdateProfileRequest
import com.sleekydz86.idolglow.user.user.ui.request.toCommand
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class UserGraphQlController(
    private val userService: UserService,
    private val userSurveyCommandService: UserSurveyCommandService,
    private val userSurveyQueryService: UserSurveyQueryService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun me(): UserProfileGraphQlResponse =
        UserProfileGraphQlResponse.from(
            userService.getUser(authenticatedUserIdResolver.resolveRequired())
        )

    @QueryMapping
    fun mySurvey(): UserSurveyGraphQlResponse =
        UserSurveyGraphQlResponse.from(
            userSurveyQueryService.findUserSurvey(authenticatedUserIdResolver.resolveRequired())
        )

    @MutationMapping
    fun updateProfile(@Argument @Valid input: UpdateProfileRequest): UserProfileGraphQlResponse =
        UserProfileGraphQlResponse.from(
            userService.updateProfile(
                userId = authenticatedUserIdResolver.resolveRequired(),
                nickname = input.nickname,
                profileImageUrl = input.profileImageUrl
            )
        )

    @MutationMapping
    fun upsertUserSurvey(@Argument @Valid input: CreateUserSurveyRequest): UserSurveyUpsertGraphQlResponse =
        UserSurveyUpsertGraphQlResponse.from(
            userSurveyCommandService.saveUserSurvey(
                userId = authenticatedUserIdResolver.resolveRequired(),
                command = input.toCommand()
            )
        )
}
