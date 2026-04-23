package com.sleekydz86.idolglow.platform.user.domain.exception

class UserAlreadyExistsException(email: String) :
    BasePlatformException("이미 가입된 이메일입니다: $email")
