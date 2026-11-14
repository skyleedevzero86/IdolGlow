package com.sleekydz86.idolglow.glowfeed.application.dto

import com.sleekydz86.idolglow.like.application.dto.LikeSummary
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "GLOW 전체 보기 카드")
data class GlowFeedItemResult(
    @field:Schema(description = "카드 고유 ID. 종류와 원본 ID 조합", example = "ARTICLE-12")
    val id: String,
    @field:Schema(description = "카테고리. trend, news, recent, local", example = "trend")
    val category: String,
    @field:Schema(description = "원본 종류. ARTICLE, EVENT, PRODUCT_REVIEW, COMMENT, COMMUNITY_POST", example = "ARTICLE")
    val kind: String,
    @field:Schema(description = "제목", example = "성수 팝업 트렌드")
    val title: String,
    @field:Schema(description = "대표 이미지. 없으면 null", example = "/uploads/idolglow/card.jpg")
    val imageUrl: String?,
    @field:Schema(description = "상세 화면 경로", example = "/glow?tab=trend&issue=vol-3&article=seongsu")
    val path: String,
    @field:Schema(description = "트렌드 기사의 발행 호. 다른 카테고리는 null", example = "3")
    val volume: Int?,
    @field:Schema(description = "카드 하단 보조 정보. 기간·장소·작성자 등", example = "2026.11.14 ~ 2026.12.01 · 세종문화회관")
    val meta: String?,
    @field:Schema(description = "태그", example = "[\"트렌드\"]")
    val tags: List<String>,
    @field:Schema(description = "정렬 기준 시각", example = "2026-11-14T10:12:17")
    val publishedAt: LocalDateTime,
    @field:Schema(description = "좋아요 API 의 targetType", example = "ARTICLE")
    val likeTargetType: String,
    @field:Schema(description = "좋아요 API 의 targetKey", example = "vol-3/seongsu")
    val likeTargetKey: String,
    @field:Schema(description = "좋아요 수", example = "4")
    val likeCount: Long,
    @field:Schema(description = "로그인 회원이 좋아요를 눌렀는지. 비회원은 false", example = "false")
    val liked: Boolean,
    @field:Schema(description = "본 회원 수(중복 제외)", example = "9")
    val viewerCount: Long,
) {
    companion object {
        fun of(
            entry: GlowFeedEntry,
            content: GlowFeedContent,
            likes: LikeSummary,
            viewerCount: Long,
        ): GlowFeedItemResult =
            GlowFeedItemResult(
                id = "${entry.kind.name}-${entry.refId}",
                category = entry.kind.category.code,
                kind = entry.kind.name,
                title = content.title,
                imageUrl = content.imageUrl,
                path = content.path,
                volume = content.volume,
                meta = content.meta,
                tags = content.tags,
                publishedAt = entry.sortAt,
                likeTargetType = entry.kind.likeTargetType.name,
                likeTargetKey = entry.likeKey,
                likeCount = likes.likeCount,
                liked = likes.liked,
                viewerCount = viewerCount,
            )
    }
}
