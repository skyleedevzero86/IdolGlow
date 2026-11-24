package com.sleekydz86.idolglow.groundedsearch.application.port

interface KeywordSearchPort {
    fun search(query: String, limit: Int): List<Long>
}
