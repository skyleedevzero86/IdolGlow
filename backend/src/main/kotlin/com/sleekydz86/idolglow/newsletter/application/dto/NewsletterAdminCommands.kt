package com.sleekydz86.idolglow.newsletter.application.dto

data class UpsertNewsletterCommand(
    val title: String,
    val categoryLabel: String,
    val publishedAt: String,
    val imageUrl: String,
    val tags: List<String>,
    val summary: String,
    val paragraphs: List<String>,
)
