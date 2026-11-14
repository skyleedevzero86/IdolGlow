package com.sleekydz86.idolglow.like.application.port.out

import com.sleekydz86.idolglow.like.domain.LikeTarget

interface LikeTargetPort {
    fun isLikeable(target: LikeTarget): Boolean
}
