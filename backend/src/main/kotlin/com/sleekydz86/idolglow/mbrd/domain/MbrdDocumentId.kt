package com.sleekydz86.idolglow.mbrd.domain

import java.util.UUID

data class MbrdDocumentId(val value: UUID) {
    fun asString(): String = value.toString()

    companion object {
        fun newId(): MbrdDocumentId = MbrdDocumentId(UUID.randomUUID())

        fun from(value: String): MbrdDocumentId = MbrdDocumentId(UUID.fromString(value))
    }
}
