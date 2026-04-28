package com.sleekydz86.idolglow.exchangerate.domain

data class ExchangeRateQuote(
    val curUnit: String,
    val curNm: String,
    val ttb: String,
    val tts: String,
    val dealBasR: String,
    val bkpr: String,
)
