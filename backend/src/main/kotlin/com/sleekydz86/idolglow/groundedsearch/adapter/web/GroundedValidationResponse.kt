package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedVerdict

data class GroundedValidationResponse(
    val accepted: Boolean,
    val rejectReason: String?,
) {
    companion object {
        fun from(verdict: GroundedVerdict): GroundedValidationResponse =
            GroundedValidationResponse(verdict.accepted, verdict.rejectReason)
    }
}
