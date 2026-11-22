package com.sleekydz86.idolglow.global.infrastructure.exception.glowcommunity

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class GlowCommunityExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    POST_NOT_FOUND(
        "GLOW_COMMUNITY_POST_NOT_FOUND",
        "게시글을 찾을 수 없습니다.",
        NOT_FOUND,
    ),
    COMMENT_NOT_FOUND(
        "GLOW_COMMUNITY_COMMENT_NOT_FOUND",
        "댓글을 찾을 수 없습니다.",
        NOT_FOUND,
    ),
    NOT_POST_OWNER(
        "GLOW_COMMUNITY_NOT_POST_OWNER",
        "본인이 작성한 글만 수정하거나 삭제할 수 있습니다.",
        FORBIDDEN,
    ),
    NOT_COMMENT_OWNER(
        "GLOW_COMMUNITY_NOT_COMMENT_OWNER",
        "본인이 작성한 댓글만 삭제할 수 있습니다.",
        FORBIDDEN,
    ),
    INVALID_TAG(
        "GLOW_COMMUNITY_INVALID_TAG",
        "말머리는 talk, question, travel 중 하나여야 합니다.",
        BAD_REQUEST,
    ),
    INVALID_SORT(
        "GLOW_COMMUNITY_INVALID_SORT",
        "정렬은 latest 또는 popular 여야 합니다.",
        BAD_REQUEST,
    ),
    BLINDED_POST(
        "GLOW_COMMUNITY_BLINDED_POST",
        "블라인드 처리된 글에는 좋아요나 댓글을 남길 수 없습니다.",
        BAD_REQUEST,
    ),
    CANNOT_REPORT_OWN_POST(
        "GLOW_COMMUNITY_CANNOT_REPORT_OWN_POST",
        "본인이 작성한 글은 신고할 수 없습니다.",
        BAD_REQUEST,
    ),
}

private const val BAD_REQUEST = 400
private const val FORBIDDEN = 403
private const val NOT_FOUND = 404
