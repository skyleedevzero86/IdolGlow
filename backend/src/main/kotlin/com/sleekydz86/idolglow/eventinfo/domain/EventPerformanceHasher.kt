package com.sleekydz86.idolglow.eventinfo.domain

import java.security.MessageDigest
import java.nio.charset.StandardCharsets

object EventPerformanceHasher {
    fun hash(item: CollectedPerformance): String {
        val canonical =
            listOf(
                item.source,
                item.sourceId,
                item.title,
                item.venue.orEmpty(),
                item.startDate?.toString().orEmpty(),
                item.endDate?.toString().orEmpty(),
                item.rawPayload,
            ).joinToString("|")
        val digest = MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
