package com.sleekydz86.idolglow.productpackage.product.ui

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductBrowseParams
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeParseException

object ProductBrowseRequestParser {

    fun parse(
        lastId: Long?,
        offset: Int?,
        size: Int,
        tag: String?,
        tags: List<String>?,
        keyword: String?,
        minPrice: String?,
        maxPrice: String?,
        visitDate: String?,
        reservableOnly: Boolean?,
        sort: String?,
        nearLatitude: String?,
        nearLongitude: String?,
        radiusMeters: Int?,
    ): ProductBrowseParams {
        val resolvedSort = parseSort(sort)
        return ProductBrowseParams(
            lastId = lastId,
            offset = offset,
            size = size,
            tag = tag?.trim()?.takeIf { it.isNotEmpty() },
            tags = tags.orEmpty().map { it.trim() }.filter { it.isNotEmpty() }.distinct(),
            keyword = keyword,
            minPrice = parseBigDecimal("minPrice", minPrice),
            maxPrice = parseBigDecimal("maxPrice", maxPrice),
            visitDate = parseVisitDate(visitDate),
            reservableOnly = reservableOnly == true,
            sort = resolvedSort,
            nearLatitude = parseBigDecimal("nearLatitude", nearLatitude),
            nearLongitude = parseBigDecimal("nearLongitude", nearLongitude),
            radiusMeters = radiusMeters,
        )
    }

    private fun parseSort(raw: String?): ProductSort {
        if (raw.isNullOrBlank()) return ProductSort.NEWEST
        return try {
            ProductSort.valueOf(raw.trim().uppercase())
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException(
                "sort는 NEWEST, POPULARITY, RATING, REVIEW_COUNT, DISTANCE 중 하나여야 합니다."
            )
        }
    }

    private fun parseBigDecimal(label: String, raw: String?): BigDecimal? {
        val t = raw?.trim() ?: return null
        if (t.isEmpty()) return null
        return try {
            BigDecimal(t)
        } catch (_: NumberFormatException) {
            throw IllegalArgumentException("${label}는 올바른 숫자 형식이어야 합니다.")
        }
    }

    private fun parseVisitDate(raw: String?): LocalDate? {
        val t = raw?.trim() ?: return null
        if (t.isEmpty()) return null
        return try {
            LocalDate.parse(t)
        } catch (_: DateTimeParseException) {
            throw IllegalArgumentException("visitDate는 ISO-8601 날짜 형식(YYYY-MM-DD)이어야 합니다.")
        }
    }

    fun fromGraphQl(
        lastId: String?,
        offset: Int?,
        size: Int,
        tag: String?,
        tags: List<String>?,
        keyword: String?,
        minPrice: String?,
        maxPrice: String?,
        visitDate: String?,
        reservableOnly: Boolean?,
        sort: ProductSort?,
        nearLatitude: String?,
        nearLongitude: String?,
        radiusMeters: Int?,
    ): ProductBrowseParams {
        val lastIdLong = when {
            lastId.isNullOrBlank() -> null
            else -> lastId.toLongOrNull()
                ?: throw IllegalArgumentException("lastId는 숫자여야 합니다.")
        }
        return parse(
            lastId = lastIdLong,
            offset = offset,
            size = size,
            tag = tag,
            tags = tags,
            keyword = keyword,
            minPrice = minPrice,
            maxPrice = maxPrice,
            visitDate = visitDate,
            reservableOnly = reservableOnly,
            sort = sort?.name,
            nearLatitude = nearLatitude,
            nearLongitude = nearLongitude,
            radiusMeters = radiusMeters,
        )
    }
}
