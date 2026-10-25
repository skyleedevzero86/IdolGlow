package com.sleekydz86.idolglow.groundedsearch.domain

import java.security.MessageDigest

object GroundedContentHash {
    fun of(content: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(content.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }
}
