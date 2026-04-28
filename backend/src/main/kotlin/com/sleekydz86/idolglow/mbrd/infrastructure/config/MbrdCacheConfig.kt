package com.sleekydz86.idolglow.mbrd.infrastructure.config

import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.cache.concurrent.ConcurrentMapCacheManager
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
@EnableCaching
class MbrdCacheConfig {
    @Bean
    @Primary
    fun mbrdCacheManager(): CacheManager =
        ConcurrentMapCacheManager(
            "mbrd-editor-document-by-id",
            "mbrd-editor-document-by-slug",
            "mbrd-editor-document-pages",
            "subway-station-llm-summary",
        )
}
