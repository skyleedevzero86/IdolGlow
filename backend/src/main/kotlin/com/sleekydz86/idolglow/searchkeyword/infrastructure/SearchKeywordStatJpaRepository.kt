package com.sleekydz86.idolglow.searchkeyword.infrastructure

import com.sleekydz86.idolglow.searchkeyword.domain.SearchKeywordStat
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface SearchKeywordStatJpaRepository : JpaRepository<SearchKeywordStat, Long> {
    fun findByKeyword(keyword: String): SearchKeywordStat?

    @Query(
        """
        select s from SearchKeywordStat s
        order by s.searchCount desc, s.lastSearchedAt desc
        """,
    )
    fun findPopular(pageable: Pageable): List<SearchKeywordStat>
}
