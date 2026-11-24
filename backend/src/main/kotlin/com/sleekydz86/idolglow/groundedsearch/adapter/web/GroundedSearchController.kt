package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.application.GroundedAnswerService
import com.sleekydz86.idolglow.groundedsearch.application.GroundedSearchReindexService
import com.sleekydz86.idolglow.groundedsearch.application.GroundedSearchService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "근거 검색", description = "상품 옵션 웹진 문단 검색")
@RestController
class GroundedSearchController(
    private val groundedSearchService: GroundedSearchService,
    private val groundedSearchReindexService: GroundedSearchReindexService,
    private val groundedAnswerService: GroundedAnswerService,
) {
    @Operation(summary = "근거 문단 검색", description = "키워드와 임베딩을 합친 상위 문단")
    @GetMapping("/api/grounded-search")
    fun search(@RequestParam q: String): ResponseEntity<List<GroundedSearchHitResponse>> =
        ResponseEntity.ok(groundedSearchService.search(q).map(GroundedSearchHitResponse::from))

    @Operation(summary = "근거 답변", description = "조건 추출, 상품 필터, 재정렬, 인용 검증 결과")
    @GetMapping("/api/grounded-search/answer")
    fun answer(@RequestParam q: String): ResponseEntity<GroundedAnswerResponse> =
        ResponseEntity.ok(GroundedAnswerResponse.from(groundedAnswerService.answer(q)))

    @Operation(summary = "근거 문단 색인", description = "본문 해시가 바뀐 문단만 다시 임베딩한다")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/admin/grounded-search/reindex")
    fun reindex(): ResponseEntity<GroundedReindexResponse> =
        ResponseEntity.ok(GroundedReindexResponse.from(groundedSearchReindexService.reindex()))
}
