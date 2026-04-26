package com.sleekydz86.idolglow.exchangerate.adapter.web.dto

import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "일일 환율 1건 (한국수출입은행 매매기준율 등)")
data class ExchangeRateItemResponse(
    @field:Schema(description = "통화코드 (예: USD, JPY(100))")
    val curUnit: String,
    @field:Schema(description = "국가/통화명")
    val curNm: String,
    @field:Schema(description = "전신환(송금) 받을 때")
    val ttb: String,
    @field:Schema(description = "전신환(송금) 보낼 때")
    val tts: String,
    @field:Schema(description = "매매 기준율")
    val dealBasR: String,
    @field:Schema(description = "장부가격")
    val bkpr: String,
) {
    companion object {
        fun from(domain: ExchangeRateQuote): ExchangeRateItemResponse =
            ExchangeRateItemResponse(
                curUnit = domain.curUnit,
                curNm = domain.curNm,
                ttb = domain.ttb,
                tts = domain.tts,
                dealBasR = domain.dealBasR,
                bkpr = domain.bkpr,
            )
    }
}
