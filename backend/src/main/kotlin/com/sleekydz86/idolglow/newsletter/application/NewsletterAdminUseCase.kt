package com.sleekydz86.idolglow.newsletter.application

import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterDetailResponse
import com.sleekydz86.idolglow.newsletter.application.dto.AdminNewsletterPageResponse
import com.sleekydz86.idolglow.newsletter.application.dto.UpsertNewsletterCommand

interface NewsletterAdminUseCase {
    fun findNewsletters(page: Int, size: Int): AdminNewsletterPageResponse
    fun findNewsletter(newsletterSlug: String): AdminNewsletterDetailResponse
    fun createNewsletter(command: UpsertNewsletterCommand): AdminNewsletterDetailResponse
    fun updateNewsletter(
        newsletterSlug: String,
        command: UpsertNewsletterCommand,
    ): AdminNewsletterDetailResponse

    fun deleteNewsletter(newsletterSlug: String)
}
