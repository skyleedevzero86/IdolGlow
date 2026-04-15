package com.sleekydz86.idolglow.pup.infrastructure

import com.sleekydz86.idolglow.pup.domain.PupItem
import com.sleekydz86.idolglow.pup.domain.PupListCriteria
import com.sleekydz86.idolglow.pup.domain.PupRepository
import com.sleekydz86.idolglow.sdom.infrastructure.SdomEntity
import jakarta.persistence.EntityManager
import jakarta.persistence.criteria.CriteriaBuilder
import jakarta.persistence.criteria.JoinType
import jakarta.persistence.criteria.Predicate
import jakarta.persistence.criteria.Root
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Repository
class PupRepositoryImpl(
    private val jpaRepository: PupJpaRepository,
    private val entityManager: EntityManager,
) : PupRepository {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findList(criteria: PupListCriteria): List<PupItem> {
        val pageable = PageRequest.of(
            criteria.pageIndex - 1,
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "frstRegistPnttm"),
        )
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(PupEntity::class.java)
        val root = cq.from(PupEntity::class.java)
        root.fetch<PupEntity, SdomEntity>("domain", JoinType.LEFT)
        cq.select(root)
        cq.where(adminListPredicate(criteria, root, cb))
        cq.orderBy(cb.desc(root.get<LocalDateTime>("frstRegistPnttm")))
        val q = entityManager.createQuery(cq)
        q.firstResult = pageable.offset.toInt()
        q.maxResults = pageable.pageSize
        return q.resultList.map(::toItem)
    }

    override fun count(criteria: PupListCriteria): Int =
        jpaRepository.count(countSpec(criteria)).toInt()

    override fun findById(popupId: String): PupItem? =
        jpaRepository.findById(popupId).map { toItem(it) }.orElse(null)

    override fun findPublicByDomain(domainId: String): List<PupItem> =
        jpaRepository.findAll(
            Specification
                .where(domainIdEquals(domainId))
                .and(noticeEnabled()),
                Sort.by(Sort.Order.desc("frstRegistPnttm")),
        ).map(::toItem)

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
        e.domainId = item.domainId?.ifBlank { null } ?: "kr"
        e.popupSjNm = item.title
        e.fileUrl = item.fileUrl
        e.linkTarget = item.linkTarget
        e.popupImgPath = item.imagePath
        e.popupFileNm = item.imageFileName
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

    private fun adminListPredicate(c: PupListCriteria, root: Root<PupEntity>, cb: CriteriaBuilder): Predicate {
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
        return p
    }

    private fun countSpec(c: PupListCriteria): Specification<PupEntity> =
        Specification { root, _, cb ->
            adminListPredicate(c, root, cb)
        }

    private fun domainIdEquals(domainId: String): Specification<PupEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<String>("domainId"), domainId.ifBlank { "kr" })
        }

    private fun noticeEnabled(): Specification<PupEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<String>("ntceAt"), "Y")
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
