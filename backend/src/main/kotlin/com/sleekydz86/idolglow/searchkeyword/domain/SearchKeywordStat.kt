package com.sleekydz86.idolglow.searchkeyword.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "search_keyword_stats")
class SearchKeywordStat(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false, unique = true, length = 100)
    var keyword: String,
    @Column(name = "search_count", nullable = false)
    var searchCount: Long = 1L,
    @Column(name = "last_searched_at", nullable = false)
    var lastSearchedAt: LocalDateTime = LocalDateTime.now(),
) : BaseEntity() {
    fun recordHit() {
        searchCount += 1L
        lastSearchedAt = LocalDateTime.now()
    }
}
