package com.sleekydz86.idolglow.groundedsearch.domain

object GroundedLinkKey {
    fun product(id: Long): String = "product/$id"

    fun option(id: Long): String = "option/$id"

    fun article(issueSlug: String, articleSlug: String): String = "idolglow/$issueSlug/$articleSlug"

    fun section(issueSlug: String, articleSlug: String, sectionId: Long): String =
        "idolglow/$issueSlug/$articleSlug#section-$sectionId"
}
