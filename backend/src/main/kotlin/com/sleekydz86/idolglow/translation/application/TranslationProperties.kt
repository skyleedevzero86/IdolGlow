package com.sleekydz86.idolglow.translation.application

import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.translation")
data class TranslationProperties(
    var enabled: Boolean = true,
    var batchSize: Int = 10,
    var maxAttempts: Int = 3,
    var maxSourceChars: Int = 8000,
    var reportThreshold: Int = 3,
    var lazyEnqueueLimit: Int = 30,
    var lookupLimit: Int = 100,
    var scanPageSize: Int = 100,
    var judgeEnabled: Boolean = true,
    var judgeMinScore: Int = 4,
    var reviewRequiredTypes: Set<TranslationContentType> =
        setOf(
            TranslationContentType.NOTICE,
            TranslationContentType.EVENT,
            TranslationContentType.PRODUCT,
            TranslationContentType.ARTICLE,
        ),
) {
    companion object {
        const val SCAN_PRIORITY: Int = 10
        const val VIEW_PRIORITY: Int = 20
    }
}
