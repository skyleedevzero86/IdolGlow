package com.sleekydz86.idolglow.glowcommunity.infrastructure

import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityPostSearchQuery
import com.sleekydz86.idolglow.glowcommunity.application.dto.GlowCommunityViewer
import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityPost
import jakarta.persistence.criteria.Predicate
import org.springframework.data.jpa.domain.Specification
import java.util.Locale

object GlowCommunityPostSpecifications {
    private const val ESCAPE = '\\'

    fun search(
        query: GlowCommunityPostSearchQuery,
        viewer: GlowCommunityViewer,
    ): Specification<GlowCommunityPost> =
        Specification { root, _, cb ->
            val predicates = mutableListOf<Predicate>(cb.isNull(root.get<Any>("deletedAt")))
            if (!viewer.admin) {
                val visible = cb.isFalse(root.get("blinded"))
                predicates +=
                    viewer.userId
                        ?.let { cb.or(visible, cb.equal(root.get<Long>("userId"), it)) }
                        ?: visible
            }
            query.tag?.let { predicates += cb.equal(root.get<Any>("tag"), it) }
            query.keyword?.let { keyword ->
                val pattern = "%${escape(keyword.lowercase(Locale.ROOT))}%"
                predicates +=
                    cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, ESCAPE),
                        cb.like(cb.lower(root.get("contentMarkdown")), pattern, ESCAPE),
                    )
            }
            cb.and(*predicates.toTypedArray())
        }

    private fun escape(value: String): String =
        value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
}
