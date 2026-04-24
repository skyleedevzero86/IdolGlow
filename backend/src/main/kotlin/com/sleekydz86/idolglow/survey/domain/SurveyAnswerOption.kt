package com.sleekydz86.idolglow.survey.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "survey_answer_option")
class SurveyAnswerOption(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "survey_answer_id", nullable = false)
    val answer: SurveyAnswer,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int,

    @Column(name = "option_text", nullable = false, length = 300)
    var optionText: String,
) : BaseEntity()
