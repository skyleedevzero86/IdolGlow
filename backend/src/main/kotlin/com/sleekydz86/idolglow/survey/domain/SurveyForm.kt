package com.sleekydz86.idolglow.survey.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "survey_form")
class SurveyForm(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(nullable = false, length = 200)
    var title: String,
    @Column(columnDefinition = "TEXT")
    var description: String? = null,
    @Column(nullable = false)
    var active: Boolean = true,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var status: SurveyFormStatus = SurveyFormStatus.SCHEDULED,
    @Enumerated(EnumType.STRING)
    @Column(name = "primary_category", nullable = false, length = 40)
    var primaryCategory: SurveyFormPrimaryCategory = SurveyFormPrimaryCategory.ALL,
    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_category", length = 40)
    var secondaryCategory: SurveyFormSecondaryCategory? = null,
    @OneToMany(
        mappedBy = "form",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    val questions: MutableList<SurveyQuestion> = mutableListOf(),
    @OneToOne(
        mappedBy = "form",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    var descriptionContent: SurveyFormDescription? = null,
) : BaseEntity() {
    fun replaceQuestions(nextQuestions: List<SurveyQuestion>) {
        questions.clear()
        questions.addAll(nextQuestions)
    }

    fun replaceDescription(
        markdown: String?,
        tagNames: List<String>,
    ) {
        val normalizedMarkdown = markdown?.trim()?.takeIf { it.isNotBlank() }
        val normalizedTags =
            tagNames
                .map { it.trim().removePrefix("#") }
                .filter { it.isNotBlank() }
                .distinct()

        description = null

        if (normalizedMarkdown == null && normalizedTags.isEmpty()) {
            descriptionContent = null
            return
        }

        val content =
            descriptionContent ?: SurveyFormDescription(
                form = this,
                markdown = normalizedMarkdown.orEmpty(),
            ).also { descriptionContent = it }
        content.markdown = normalizedMarkdown.orEmpty()
        content.replaceTags(normalizedTags)
    }
}
