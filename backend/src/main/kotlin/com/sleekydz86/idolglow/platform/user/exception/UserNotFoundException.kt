package com.sleekydz86.idolglow.platform.user.exception

class UserNotFoundException(email: String) :
    BasePlatformException("사용자를 찾을 수 없습니다: $email")
