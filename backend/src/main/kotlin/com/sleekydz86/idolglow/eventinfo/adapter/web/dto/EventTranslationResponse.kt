package com.sleekydz86.idolglow.eventinfo.adapter.web.dto

import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation

data class EventTranslationResponse(
    val globalId: String,
    val locale: String,
    val title: String,
    val origin: String,
) {
    companion object {
        fun from(translation: EventTranslation): EventTranslationResponse =
            EventTranslationResponse(
                globalId = translation.globalId,
                locale = translation.locale,
                title = translation.title,
                origin = translation.origin.name,
            )
    }
}
