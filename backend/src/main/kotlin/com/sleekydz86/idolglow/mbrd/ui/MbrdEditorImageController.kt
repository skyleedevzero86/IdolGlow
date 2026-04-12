package com.sleekydz86.idolglow.mbrd.ui

import com.sleekydz86.idolglow.mbrd.infrastructure.storage.MbrdMinioEditorImageStorageService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.io.InputStreamResource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import java.nio.charset.StandardCharsets
import java.util.UUID

@Tag(name = "mbrd editor images", description = "마크다운 에디터 이미지 업로드 및 조회")
@Validated
@RestController
@RequestMapping("/api/mbrd/editor/images")
class MbrdEditorImageController(
    private val minioStorageService: ObjectProvider<MbrdMinioEditorImageStorageService>,
) {
    @Operation(summary = "이미지 업로드")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun uploadImage(@RequestPart("file") file: MultipartFile) =
        storage().upload(file)

    @Operation(summary = "이미지 조회")
    @GetMapping("/{assetId}")
    fun loadImage(@PathVariable assetId: UUID): ResponseEntity<InputStreamResource> {
        val image = storage().load(assetId)
        val disposition = ContentDisposition.inline()
            .filename(image.originalFileName, StandardCharsets.UTF_8)
            .build()
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
            .contentType(MediaType.parseMediaType(image.contentType))
            .contentLength(image.size)
            .body(InputStreamResource(image.stream))
    }

    private fun storage(): MbrdMinioEditorImageStorageService =
        minioStorageService.getIfAvailable()
            ?: throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "MinIO 가 비활성화되어 있거나 MinioClient 빈이 없어 에디터 이미지 기능을 사용할 수 없습니다.",
            )
}
