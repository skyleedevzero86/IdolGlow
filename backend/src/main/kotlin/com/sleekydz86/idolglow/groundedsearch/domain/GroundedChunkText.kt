package com.sleekydz86.idolglow.groundedsearch.domain

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSection

object GroundedChunkText {
    fun fromProduct(product: Product): String =
        join(
            product.name,
            product.description,
            product.productTags.joinToString(" ") { it.tagName },
            product.productLocation?.name,
            product.productLocation?.roadAddressName,
        )

    fun fromOption(option: Option): String = join(option.name, option.description, option.location)

    fun fromArticle(article: WebzineArticle): String = join(article.title, article.summary)

    fun fromSection(section: WebzineArticleSection): String = join(section.heading, section.body)

    private fun join(vararg parts: String?): String =
        parts.mapNotNull { it?.trim() }.filter { it.isNotEmpty() }.joinToString("\n")
}
