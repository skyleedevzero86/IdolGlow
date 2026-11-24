package com.sleekydz86.idolglow.groundedsearch.infrastructure

import com.sleekydz86.idolglow.groundedsearch.domain.RagChunk
import com.sleekydz86.idolglow.groundedsearch.domain.RagSourceType
import org.springframework.data.jpa.repository.JpaRepository

interface RagChunkJpaRepository : JpaRepository<RagChunk, Long> {
    fun findBySourceTypeAndSourceId(sourceType: RagSourceType, sourceId: Long): RagChunk?
}
