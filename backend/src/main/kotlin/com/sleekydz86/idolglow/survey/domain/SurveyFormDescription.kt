package com.sleekydz86.idolglow.survey.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "survey_form_description")
class SurveyFormDescription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_form_id", nullable = false, unique = true)
    val form: SurveyForm,

    @Column(columnDefinition = "TEXT", nullable = false)
    var markdown: String,

    @OneToMany(
        mappedBy = "description",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    val tags: MutableList<SurveyFormDescriptionTag> = mutableListOf(),
) : BaseEntity() {
    fun replaceTags(tagNames: List<String>) {
        tags.clear()
        tags.addAll(
            tagNames.mapIndexed { index, tag ->
                SurveyFormDescriptionTag(
                    description = this,
                    displayOrder = index + 1,
                    tagName = tag,
                )
            },
        )
    }
}
