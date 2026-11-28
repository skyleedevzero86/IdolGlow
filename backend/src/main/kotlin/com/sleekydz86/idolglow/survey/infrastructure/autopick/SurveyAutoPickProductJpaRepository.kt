package com.sleekydz86.idolglow.survey.infrastructure.autopick

import com.sleekydz86.idolglow.survey.domain.autopick.SurveyAutoPickProduct
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SurveyAutoPickProductJpaRepository : JpaRepository<SurveyAutoPickProduct, Long> {
    fun findAllBySettingId(settingId: Long): List<SurveyAutoPickProduct>

    fun findAllBySettingIdIn(settingIds: Collection<Long>): List<SurveyAutoPickProduct>

    fun existsBySettingIdAndProductId(
        settingId: Long,
        productId: Long,
    ): Boolean

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from SurveyAutoPickProduct p where p.settingId = :settingId")
    fun deleteAllBySettingId(
        @Param("settingId") settingId: Long,
    )
}
