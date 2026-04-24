package com.sleekydz86.idolglow.productpackage.option.application.dto

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import org.springframework.data.domain.Page

data class OptionPageResponse(
    val content: List<OptionResponse>,
    val totalElements: Long,
    val totalPages: Int,
    val number: Int,
    val size: Int,
) {
    companion object {
        fun from(page: Page<Option>): OptionPageResponse =
            OptionPageResponse(
                content = page.content.map(OptionResponse::from),
                totalElements = page.totalElements,
                totalPages = page.totalPages,
                number = page.number,
                size = page.size,
            )
    }
}
