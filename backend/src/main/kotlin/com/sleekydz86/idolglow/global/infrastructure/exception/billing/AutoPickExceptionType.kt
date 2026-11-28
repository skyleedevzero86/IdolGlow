package com.sleekydz86.idolglow.global.infrastructure.exception.billing

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomExceptionType

enum class AutoPickExceptionType(
    override val errorCode: String,
    override val message: String,
    override val httpStatusCode: Int,
) : CustomExceptionType {
    MEMBERSHIP_REQUIRED(
        "AUTO_PICK_MEMBERSHIP_REQUIRED",
        "자동 담기는 Glow Pass 회원만 켤 수 있습니다.",
        FORBIDDEN,
    ),
    INVALID_PRODUCTS(
        "AUTO_PICK_INVALID_PRODUCTS",
        "자동 담기 상품은 1개 이상 10개 이하로 골라 주세요.",
        BAD_REQUEST,
    ),
    INVALID_MAX_PRICE(
        "AUTO_PICK_INVALID_MAX_PRICE",
        "예산 상한은 0원 이상이어야 합니다.",
        BAD_REQUEST,
    ),
    PRODUCT_NOT_FOUND(
        "AUTO_PICK_PRODUCT_NOT_FOUND",
        "자동 담기 상품을 찾을 수 없습니다.",
        NOT_FOUND,
    ),
    CANDIDATE_NOT_FOUND(
        "AUTO_PICK_CANDIDATE_NOT_FOUND",
        "자동 담기 예정 내역을 찾을 수 없습니다.",
        NOT_FOUND,
    ),
    SKIP_CLOSED(
        "AUTO_PICK_SKIP_CLOSED",
        "결제 예정 시각이 지나 건너뛸 수 없습니다.",
        CONFLICT,
    ),
}

private const val BAD_REQUEST = 400
private const val FORBIDDEN = 403
private const val NOT_FOUND = 404
private const val CONFLICT = 409
