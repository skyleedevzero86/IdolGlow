package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.survey.domain.SurveyForm
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
        where lower(f.title) like lower(concat('%', :keyword, '%'))
           or lower(coalesce(f.description, '')) like lower(concat('%', :keyword, '%'))
        order by f.updatedAt desc
        """,
    )
    fun searchByKeyword(@Param("keyword") keyword: String): List<SurveyForm>

    @Query(
        value = """
        select f
        from SurveyForm f
        where lower(f.title) like lower(concat('%', :keyword, '%'))
           or lower(coalesce(f.description, '')) like lower(concat('%', :keyword, '%'))
        order by f.updatedAt desc
        """,
        countQuery = """
        select count(f)
        from SurveyForm f
        where lower(f.title) like lower(concat('%', :keyword, '%'))
           or lower(coalesce(f.description, '')) like lower(concat('%', :keyword, '%'))
        """,
    )
    fun searchByKeyword(
        @Param("keyword") keyword: String,
        pageable: Pageable,
    ): Page<SurveyForm>
}
