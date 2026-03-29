package com.sleekydz86.idolglow.user.user.domain

import com.sleekydz86.idolglow.global.BaseEntity
import com.sleekydz86.idolglow.user.user.application.dto.UpsertUserSurveyCommand
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import com.sleekydz86.idolglow.user.user.domain.vo.Places
import jakarta.persistence.*
import java.time.LocalDate

@Entity
@Table(
    name = "user_survey",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_survey_user_id", columnNames = ["user_id"])
    ],
    indexes = [
        Index(name = "idx_user_survey_user_id", columnList = "user_id")
    ]
)
class UserSurvey(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    val user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var concept: ConceptType,

    @Column(name = "idol_name", nullable = false)
    var idolName: String,

    @Column(name = "visit_start_date", nullable = false)
    var visitStartDate: LocalDate,

    @Column(name = "visit_end_date", nullable = false)
    var visitEndDate: LocalDate,

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "user_survey_places",
        joinColumns = [JoinColumn(name = "user_survey_id")],
        indexes = [
            Index(name = "idx_user_survey_places_survey_id", columnList = "user_survey_id")
        ]
    )
    @Column(name = "place", nullable = false, length = 100)
    val places: MutableList<String> = mutableListOf()
) : BaseEntity() {

    fun update(command: UpsertUserSurveyCommand) {
        apply(
            concept = command.concept,
            idolName = command.idolName,
            visitStartDate = command.visitStartDate,
            visitEndDate = command.visitEndDate,
            places = Places.of(command.places)
        )
    }

    fun apply(
        concept: ConceptType,
        idolName: String,
        visitStartDate: LocalDate,
        visitEndDate: LocalDate,
        places: Places
    ) {
        require(!visitEndDate.isBefore(visitStartDate)) {
            "방문 종료일은 방문 시작일과 같거나 이후여야 합니다."
        }

        this.concept = concept
        this.idolName = idolName.trim()
        this.visitStartDate = visitStartDate
        this.visitEndDate = visitEndDate
        places.applyTo(this.places)
    }

    companion object {
        private fun normalizePlaces(places: List<String>): List<String> =
            places.map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .toList()

        fun of(
            user: User,
            concept: ConceptType,
            idolName: String,
            visitStartDate: LocalDate,
            visitEndDate: LocalDate,
            places: List<String>
        ): UserSurvey {
            val survey = UserSurvey(
                user = user,
                concept = concept,
                idolName = idolName.trim(),
                visitStartDate = visitStartDate,
                visitEndDate = visitEndDate
            )

            survey.apply(
                concept = concept,
                idolName = idolName,
                visitStartDate = visitStartDate,
                visitEndDate = visitEndDate,
                places = Places.of(places)
            )

            return survey
        }
    }
}
