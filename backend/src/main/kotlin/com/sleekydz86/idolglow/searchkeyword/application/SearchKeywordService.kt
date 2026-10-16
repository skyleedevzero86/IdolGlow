package com.sleekydz86.idolglow.searchkeyword.application

import com.sleekydz86.idolglow.searchkeyword.application.dto.PopularSearchKeywordResponse
import com.sleekydz86.idolglow.searchkeyword.domain.SearchKeywordStat
import com.sleekydz86.idolglow.searchkeyword.infrastructure.SearchKeywordStatJpaRepository
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.text.Normalizer

@Service
class SearchKeywordService(
    private val searchKeywordStatJpaRepository: SearchKeywordStatJpaRepository,
) {
    @Transactional(readOnly = true)
    fun findPopular(limit: Int): List<PopularSearchKeywordResponse> {
        val size = limit.coerceIn(1, 60)
        return searchKeywordStatJpaRepository
            .findPopular(PageRequest.of(0, size))
            .map { PopularSearchKeywordResponse(keyword = it.keyword, searchCount = it.searchCount) }
    }

    @Transactional
    fun record(rawKeyword: String) {
        val keyword = normalize(rawKeyword) ?: return
        val existing = searchKeywordStatJpaRepository.findByKeyword(keyword)
        if (existing != null) {
            existing.recordHit()
            return
        }
        searchKeywordStatJpaRepository.save(SearchKeywordStat(keyword = keyword))
    }

    private fun normalize(raw: String): String? {
        val collapsed =
            Normalizer
                .normalize(raw.trim(), Normalizer.Form.NFKC)
                .replace(WHITESPACE, " ")
                .trim()
                .lowercase()
        if (collapsed.isEmpty() || collapsed.length > 100) {
            return null
        }
        return collapsed
    }

    companion object {
        private val WHITESPACE = Regex("\\s+")
    }
}
