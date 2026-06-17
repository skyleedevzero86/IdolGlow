package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SurveyFormJpaRepository : JpaRepository<SurveyForm, Long> {
    fun findFirstByActiveTrueOrderByIdDesc(): SurveyForm?

    fun findAllByOrderByUpdatedAtDesc(): List<SurveyForm>

    fun findAllByOrderByUpdatedAtDesc(pageable: Pageable): Page<SurveyForm>

    @Query(
        """
        select distinct f
        from SurveyForm f
        left join fetch f.questions q
        left join f.descriptionContent d
        where (:keyword is null
              or lower(f.title) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(d.markdown, f.description, '')) like lower(concat('%', :keyword, '%')))
          and (:status is null or f.status = :status)
          and (:primaryCategory is null or f.primaryCategory = :primaryCategory)
          and (:secondaryCategory is null or f.secondaryCategory = :secondaryCategory)
        order by f.updatedAt desc
        """,
    )
    fun search(
        @Param("keyword") keyword: String?,
        @Param("status") status: SurveyFormStatus?,
        @Param("primaryCategory") primaryCategory: SurveyFormPrimaryCategory?,
        @Param("secondaryCategory") secondaryCategory: SurveyFormSecondaryCategory?,
    ): List<SurveyForm>

    @Query(
        value = """
        select f
        from SurveyForm f
        left join f.descriptionContent d
        where (:keyword is null
              or lower(f.title) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(d.markdown, f.description, '')) like lower(concat('%', :keyword, '%')))
          and (:status is null or f.status = :status)
          and (:primaryCategory is null or f.primaryCategory = :primaryCategory)
          and (:secondaryCategory is null or f.secondaryCategory = :secondaryCategory)
        order by f.updatedAt desc
        """,
        countQuery = """
        select count(f)
        from SurveyForm f
        left join f.descriptionContent d
        where (:keyword is null
              or lower(f.title) like lower(concat('%', :keyword, '%'))
              or lower(coalesce(d.markdown, f.description, '')) like lower(concat('%', :keyword, '%')))
          and (:status is null or f.status = :status)
          and (:primaryCategory is null or f.primaryCategory = :primaryCategory)
          and (:secondaryCategory is null or f.secondaryCategory = :secondaryCategory)
        """,
    )
    fun search(
        @Param("keyword") keyword: String?,
        @Param("status") status: SurveyFormStatus?,
        @Param("primaryCategory") primaryCategory: SurveyFormPrimaryCategory?,
        @Param("secondaryCategory") secondaryCategory: SurveyFormSecondaryCategory?,
        pageable: Pageable,
    ): Page<SurveyForm>
}
