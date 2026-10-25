package com.sleekydz86.idolglow.eventinfo.adapter.mcp

import com.sleekydz86.idolglow.eventinfo.application.EventMatchComparisonView
import com.sleekydz86.idolglow.eventinfo.application.EventMatchConflictView
import com.sleekydz86.idolglow.eventinfo.application.EventMatchProposalView
import com.sleekydz86.idolglow.eventinfo.application.EventMatchReviewService
import org.springframework.ai.mcp.annotation.McpTool
import org.springframework.ai.mcp.annotation.McpToolParam
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(name = ["spring.ai.mcp.server.enabled"], havingValue = "true")
class EventMatchMcpTools(
    private val eventMatchReviewService: EventMatchReviewService,
) {
    @McpTool(
        name = "list_match_conflicts",
        description = "CANDIDATE와 CONFLICT 공연 매칭 목록을 조회한다. 개인정보와 키는 포함하지 않는다.",
    )
    fun listMatchConflicts(): List<EventMatchConflictView> = eventMatchReviewService.listConflicts()

    @McpTool(
        name = "compare_source_records",
        description = "공급처 원본 두 건의 제목, 기간, 장소, contentId를 나란히 비교한다.",
    )
    fun compareSourceRecords(
        @McpToolParam(description = "왼쪽 대상. 형식은 공급처|공급처ID", required = true) left: String,
        @McpToolParam(description = "오른쪽 대상. 형식은 공급처|공급처ID", required = true) right: String,
    ): EventMatchComparisonView = eventMatchReviewService.compare(left, right)

    @McpTool(
        name = "propose_merge",
        description = "병합 요청만 만든다. 적용은 하지 않으며 사람이 승인한 뒤에만 반영된다.",
    )
    fun proposeMerge(
        @McpToolParam(description = "병합할 대상. 형식은 공급처|공급처ID", required = true) target: String,
        @McpToolParam(description = "합칠 글로벌 공연 ID", required = true) globalId: String,
        @McpToolParam(description = "병합 근거. 비우면 서버가 기간·장소·제목으로 적는다.", required = false) reason: String?,
    ): EventMatchProposalView = eventMatchReviewService.proposeMerge(target, globalId, reason)
}
