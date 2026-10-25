package com.sleekydz86.idolglow.groundedsearch.application.port

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChunkDraft

interface GroundedSourcePort {
    fun readAll(): List<GroundedChunkDraft>
}
