package com.sleekydz86.idolglow.mbrd.infrastructure.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class MbrdClockConfig {
    @Bean
    fun mbrdClock(): Clock = Clock.systemUTC()
}
