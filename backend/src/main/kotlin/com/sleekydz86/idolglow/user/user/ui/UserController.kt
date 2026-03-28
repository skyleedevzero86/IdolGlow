package com.sleekydz86.idolglow.user.user.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.user.user.application.UserService
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.ui.request.UpdateProfileRequest
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService
) : UserApi {

    @PatchMapping
    override fun updateProfile(
        @LoginUser userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<GetUserLoginInfoResponse> =
        ResponseEntity.ok(userService.updateProfile(userId, request.nickname, request.profileImageUrl))
}
