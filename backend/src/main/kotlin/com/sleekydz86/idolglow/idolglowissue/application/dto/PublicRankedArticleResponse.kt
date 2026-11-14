package com.sleekydz86.idolglow.idolglowissue.application.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "공개 트렌드 기사 카드")
data class PublicRankedArticleResponse(
    @field:Schema(description = "호 슬러그", example = "vol-3")
    val issueSlug: String,
    @field:Schema(description = "발행 호", example = "3")
    val volume: Int,
    @field:Schema(description = "발행 연월", example = "2026.11.")
    val issueDate: String,
    @field:Schema(description = "기사 슬러그", example = "seongsu-popup")
    val articleSlug: String,
    @field:Schema(description = "제목")
    val title: String,
    @field:Schema(description = "요약")
    val summary: String,
    @field:Schema(description = "카드 이미지")
    val cardImageUrl: String,
    @field:Schema(description = "분류 코드", example = "exhibition")
    val category: String,
    @field:Schema(description = "분류 이름", example = "전시")
    val categoryLabel: String,
    @field:Schema(description = "태그")
    val tags: List<String>,
    @field:Schema(description = "좋아요 API 의 targetType", example = "ARTICLE")
    val likeTargetType: String,
    @field:Schema(description = "좋아요 API 의 targetKey. 호슬러그/기사슬러그", example = "vol-3/seongsu-popup")
    val likeTargetKey: String,
    @field:Schema(description = "좋아요 수", example = "4")
    val likeCount: Long,
    @field:Schema(description = "본 회원 수(중복 제외)", example = "9")
    val viewerCount: Long,
    @field:Schema(description = "로그인 회원이 좋아요를 눌렀는지. 비회원은 false", example = "false")
    val liked: Boolean,
) {
    companion object {
        const val LIKE_TARGET_TYPE = "ARTICLE"

        fun of(
            article: RankedIdolGlowArticle,
            liked: Boolean,
        ): PublicRankedArticleResponse =
            PublicRankedArticleResponse(
                issueSlug = article.issueSlug,
                volume = article.volume,
                issueDate = article.issueDate.asIssueDisplayValue(),
                articleSlug = article.articleSlug,
                title = article.title,
                summary = article.summary,
                cardImageUrl = article.cardImageUrl,
                category = article.category.code,
                categoryLabel = article.category.label,
                tags = article.tags,
                likeTargetType = LIKE_TARGET_TYPE,
                likeTargetKey = article.contentKey,
                likeCount = article.likeCount,
                viewerCount = article.viewerCount,
                liked = liked,
            )
    }
}
