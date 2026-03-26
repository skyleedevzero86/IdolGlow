package com.sleekydz86.idolglow.user.user.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.user.user.application.UserService
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
    override fun updateNickname(
        @LoginUser userId: Long,
        @Valid @RequestBody request: UpdateNicknameRequest
    ): ResponseEntity<GetUserLoginInfoResponse> =
        ResponseEntity.ok(userService.updateNickname(userId, request.nickname))
}
