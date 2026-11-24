package com.sleekydz86.idolglow.searchkeyword.adapter.web

import com.sleekydz86.idolglow.searchkeyword.application.SearchKeywordService
import com.sleekydz86.idolglow.searchkeyword.application.dto.PopularSearchKeywordResponse
import com.sleekydz86.idolglow.searchkeyword.application.dto.RecordSearchKeywordRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "검색어", description = "전체 이용자 검색어 집계 API")
@RestController
@RequestMapping("/search-keywords")
class SearchKeywordController(
    private val searchKeywordService: SearchKeywordService,
) {
    @Operation(summary = "자주 찾는 검색어 조회")
    @GetMapping("/popular")
    fun popular(
        @RequestParam(defaultValue = "6") limit: Int,
    ): ResponseEntity<List<PopularSearchKeywordResponse>> =
        ResponseEntity.ok(searchKeywordService.findPopular(limit))

    @Operation(summary = "검색어 기록")
    @PostMapping
    fun record(
        @Valid @RequestBody request: RecordSearchKeywordRequest,
    ): ResponseEntity<Void> {
        searchKeywordService.record(request.keyword)
        return ResponseEntity.noContent().build()
    }
}
