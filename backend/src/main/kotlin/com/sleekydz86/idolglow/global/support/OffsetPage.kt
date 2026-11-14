package com.sleekydz86.idolglow.global.support

data class OffsetPageRequest(
    val page: Int,
    val size: Int,
) {
    companion object {
        const val DEFAULT_SIZE = 20
        private const val MAX_SIZE = 50

        fun of(
            page: Int,
            size: Int,
        ): OffsetPageRequest = OffsetPageRequest(page.coerceAtLeast(0), size.coerceIn(1, MAX_SIZE))
    }
}

data class OffsetSlice<T>(
    val items: List<T>,
    val totalElements: Long,
)

data class OffsetPageResult<T>(
    val items: List<T>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun <T> of(
            items: List<T>,
            request: OffsetPageRequest,
            totalElements: Long,
        ): OffsetPageResult<T> {
            val totalPages = ((totalElements + request.size - 1) / request.size).toInt().coerceAtLeast(1)
            return OffsetPageResult(
                items = items,
                page = request.page,
                size = request.size,
                totalElements = totalElements,
                totalPages = totalPages,
                hasNext = (request.page + 1).toLong() * request.size < totalElements,
            )
        }
    }
}
