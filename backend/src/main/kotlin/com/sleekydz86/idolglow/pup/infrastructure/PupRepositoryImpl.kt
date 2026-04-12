package com.sleekydz86.idolglow.pup.infrastructure

import com.sleekydz86.idolglow.pup.domain.PupItem
import com.sleekydz86.idolglow.pup.domain.PupListCriteria
import com.sleekydz86.idolglow.pup.domain.PupRepository
import com.sleekydz86.idolglow.sdom.infrastructure.SdomEntity
import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import java.time.format.DateTimeFormatter

@Repository
class PupRepositoryImpl(
    private val jpaRepository: PupJpaRepository,
) : PupRepository {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findList(criteria: PupListCriteria): List<PupItem> {
        val pageable = PageRequest.of(
            criteria.pageIndex - 1,
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "frstRegistPnttm"),
        )
        return jpaRepository.findAll(specFrom(criteria), pageable).content.map { toItem(it) }
    }

    override fun count(criteria: PupListCriteria): Int =
        jpaRepository.count(specFrom(criteria)).toInt()

    override fun findById(popupId: String): PupItem? =
        jpaRepository.findById(popupId).map { toItem(it) }.orElse(null)

    override fun insert(item: PupItem) {
        val e = PupEntity(
            popupId = item.popupId,
            domainId = item.domainId?.ifBlank { null } ?: "kr",
            domain = null,
            popupSjNm = item.title,
            fileUrl = item.fileUrl,
            linkTarget = item.linkTarget ?: "_blank",
            popupImgPath = item.imagePath,
            popupFileNm = item.imageFileName,
            popupVrticlLc = 0,
            popupWidthLc = 0,
            popupVrticlSize = 400,
            popupWidthSize = 500,
            ntceBgnde = item.noticeStartDate,
            ntceEndde = item.noticeEndDate,
            stopvewSetupAt = item.stopViewYn ?: "Y",
            ntceAt = item.noticeYn ?: "Y",
            frstRegisterId = item.createdBy,
            frstRegistPnttm = java.time.LocalDateTime.now(),
            lastUpdusrId = null,
            lastUpdtPnttm = null,
        )
        jpaRepository.save(e)
    }

    override fun update(item: PupItem) {
        val e = jpaRepository.findById(item.popupId).orElseThrow()
        e.popupSjNm = item.title
        e.fileUrl = item.fileUrl
        e.linkTarget = item.linkTarget
        e.ntceBgnde = item.noticeStartDate
        e.ntceEndde = item.noticeEndDate
        e.stopvewSetupAt = item.stopViewYn
        e.ntceAt = item.noticeYn
        e.lastUpdusrId = item.updatedBy
        e.lastUpdtPnttm = java.time.LocalDateTime.now()
        jpaRepository.save(e)
    }

    override fun delete(popupId: String) {
        jpaRepository.deleteById(popupId)
    }

    private fun specFrom(c: PupListCriteria): Specification<PupEntity> =
        Specification { root, query, cb ->
            val isCountQuery = query?.resultType == Long::class.java ||
                query?.resultType == java.lang.Long::class.javaPrimitiveType
            if (!isCountQuery) {
                root.fetch<PupEntity, SdomEntity>("domain", JoinType.LEFT)
            }
            val domainId = c.domainId.ifBlank { "kr" }
            val keyword = c.keyword
            val searchType = c.searchType
            var p = cb.equal(root.get<String>("domainId"), domainId)
            if (searchType == "title" && !keyword.isNullOrBlank()) {
                p = cb.and(p, cb.like(root.get("popupSjNm"), "%$keyword%"))
            }
            if (searchType == "fileUrl" && !keyword.isNullOrBlank()) {
                p = cb.and(p, cb.like(root.get("fileUrl"), "%$keyword%"))
            }
            p
        }

    private fun toItem(e: PupEntity): PupItem =
        PupItem(
            popupId = e.popupId,
            domainId = e.domainId,
            title = e.popupSjNm,
            fileUrl = e.fileUrl,
            linkTarget = e.linkTarget,
            imagePath = e.popupImgPath,
            imageFileName = e.popupFileNm,
            noticeStartDate = e.ntceBgnde,
            noticeEndDate = e.ntceEndde,
            stopViewYn = e.stopvewSetupAt,
            noticeYn = e.ntceAt,
            createdBy = e.frstRegisterId,
            createdAtFormatted = e.frstRegistPnttm?.format(fmt),
            updatedBy = e.lastUpdusrId,
            updatedAtFormatted = e.lastUpdtPnttm?.format(fmt),
            domainName = e.domain?.domainNm,
        )
}
