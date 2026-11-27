package com.sleekydz86.idolglow.luckydraw.application

import org.springframework.stereotype.Component
import java.security.SecureRandom

@Component
class LuckyDrawSeedSource {
    private val random = SecureRandom()

    internal var nextSeedOverride: (() -> Long)? = null

    fun nextSeed(): Long = nextSeedOverride?.invoke() ?: random.nextLong()
}
