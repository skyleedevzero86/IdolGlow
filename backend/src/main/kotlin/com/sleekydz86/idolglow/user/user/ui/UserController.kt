package com.sleekydz86.idolglow.user.user.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.user.user.application.UserProfileImageService
import com.sleekydz86.idolglow.user.user.application.UserService
import com.sleekydz86.idolglow.user.user.application.dto.GetUserLoginInfoResponse
import com.sleekydz86.idolglow.user.user.ui.request.ChangePasswordRequest
import com.sleekydz86.idolglow.user.user.ui.request.UpdateProfileRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val userProfileImageService: UserProfileImageService,
) : UserApi {

    @PatchMapping
    override fun updateProfile(
        @LoginUser userId: Long,
        @Valid @RequestBody request: UpdateProfileRequest
    ): ResponseEntity<GetUserLoginInfoResponse> =
        ResponseEntity.ok(userService.updateProfile(userId, request.nickname, request.profileImageUrl))

    @PostMapping("/profile-image", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    override fun uploadProfileImage(
        @LoginUser userId: Long,
        @RequestPart("file") file: MultipartFile,
    ): ResponseEntity<GetUserLoginInfoResponse> {
        val url = userProfileImageService.uploadAndGetPublicUrl(userId, file)
        return ResponseEntity.ok(userService.updateProfile(userId, null, url))
    }

    @PatchMapping("/password")
    override fun changePassword(
        @LoginUser userId: Long,
        @Valid @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<Unit> {
        userService.changePassword(userId, request.currentPassword, request.newPassword)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }
}
