package com.sleekydz86.idolglow.platform.user.exception

class AuthenticationFailedException(email: String) :
    BasePlatformException("로그인에 실패했습니다: $email")
