package com.sleekydz86.idolglow.admin.ui.dto

import io.swagger.v3.oas.annotations.media.Schema

data class AdminSubscriptionOverviewResponse(
    @Schema(description = "활성 구독 건수")
    val totalActive: Long = 0,
    val totalSubscribers: Long = 0,
    val newsletterSubscriberCount: Long = 0,
    val issueSubscriberCount: Long = 0,
    val totalDispatches: Long = 0,
    val subscribers: List<AdminSubscriptionSubscriberResponse> = emptyList(),
    val subscriberPage: Int = 1,
    val subscriberSize: Int = 10,
    val subscriberTotalElements: Long = 0,
    val subscriberTotalPages: Int = 0,
    val subscriberHasNext: Boolean = false,
    val dispatches: List<AdminSubscriptionDispatchResponse> = emptyList(),
    val dispatchPage: Int = 1,
    val dispatchSize: Int = 10,
    val dispatchTotalElements: Long = 0,
    val dispatchTotalPages: Int = 0,
    val dispatchHasNext: Boolean = false,
    val schedules: List<AdminSubscriptionScheduleResponse> = emptyList(),
    val latestContents: List<AdminSubscriptionLatestContentResponse> = emptyList(),
) {
    companion object
}
