package com.sleekydz86.idolglow.mbrd.ui

import com.sleekydz86.idolglow.mbrd.application.MbrdEditorBootstrapService
import com.sleekydz86.idolglow.mbrd.ui.dto.MbrdSaveDraftRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@Tag(name = "마크다운 에디터", description = "마크다운 에디터 게시판 REST API")
@Validated
@RestController
@RequestMapping("/api/mbrd/editor")
class MbrdEditorRestController(
    private val editorBootstrapService: MbrdEditorBootstrapService,
) {
    @Operation(summary = "에디터 초기 데이터 조회")
    @GetMapping("/bootstrap")
    fun loadBootstrap() = editorBootstrapService.loadBootstrap()

    @Operation(summary = "문서 목록 조회")
    @GetMapping("/documents")
    fun listDocuments(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "25") size: Int,
        @RequestParam(defaultValue = "") query: String,
        @RequestParam(defaultValue = "published") status: String,
    ) = editorBootstrapService.list(page, size, query, status)

    @Operation(summary = "문서 상세 조회")
    @GetMapping("/documents/{documentId}")
    fun loadDocument(@PathVariable documentId: String) =
        editorBootstrapService.loadDocument(documentId)

    @Operation(summary = "URL slug 로 출간 문서 상세 조회")
    @GetMapping("/documents/slug/{urlSlug}")
    fun loadPublishedDocumentByUrlSlug(@PathVariable urlSlug: String) =
        editorBootstrapService.loadPublishedDocumentByUrlSlug(urlSlug)

    @Operation(summary = "문서 저장")
    @PostMapping("/draft")
    @ResponseStatus(HttpStatus.CREATED)
    fun saveDraft(@Valid @RequestBody request: MbrdSaveDraftRequest) =
        editorBootstrapService.save(request.toCommand())

    @Operation(summary = "문서 삭제")
    @DeleteMapping("/documents/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteDocument(@PathVariable documentId: String) {
        editorBootstrapService.deleteDocument(documentId)
    }
}
