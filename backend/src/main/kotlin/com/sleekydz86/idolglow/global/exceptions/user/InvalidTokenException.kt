package com.sleekydz86.idolglow.platform.user.exception

class InvalidTokenException(tokenKind: String) :
    BasePlatformException("유효하지 않은 토큰입니다: $tokenKind")
