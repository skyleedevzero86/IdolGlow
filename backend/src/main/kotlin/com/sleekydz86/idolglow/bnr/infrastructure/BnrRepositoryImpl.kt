package com.sleekydz86.idolglow.bnr.infrastructure

import com.sleekydz86.idolglow.bnr.domain.BnrItem
import com.sleekydz86.idolglow.bnr.domain.BnrListCriteria
import com.sleekydz86.idolglow.bnr.domain.BnrRepository
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
class BnrRepositoryImpl(
    private val jpaRepository: BnrJpaRepository,
    private val entityManager: EntityManager,
) : BnrRepository {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findList(criteria: BnrListCriteria): List<BnrItem> {
        val pageable = PageRequest.of(
            criteria.pageIndex - 1,
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "frstRegistPnttm"),
        )
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(BnrEntity::class.java)
        val root = cq.from(BnrEntity::class.java)
        root.fetch<BnrEntity, SdomEntity>("domain", JoinType.LEFT)
        cq.select(root).distinct(true)
        cq.where(adminListPredicate(criteria, root, cb))
        cq.orderBy(cb.desc(root.get<LocalDateTime>("frstRegistPnttm")))
        val q = entityManager.createQuery(cq)
        q.firstResult = pageable.offset.toInt()
        q.maxResults = pageable.pageSize
        return q.resultList.map(::toItem)
    }

    override fun count(criteria: BnrListCriteria): Int =
        jpaRepository.count(countSpec(criteria)).toInt()

    override fun findById(bannerId: String): BnrItem? =
        jpaRepository.findById(bannerId).map { toItem(it) }.orElse(null)

    override fun findActiveByDomain(domainId: String): List<BnrItem> =
        jpaRepository.findAll(
            Specification
                .where(domainIdEquals(domainId))
                .and(activeEquals()),
            Sort.by(
                Sort.Order.asc("sortOrdr"),
                Sort.Order.desc("frstRegistPnttm"),
            ),
        ).map(::toItem)

    override fun insert(item: BnrItem) {
        val e = BnrEntity(
            bannerId = item.bannerId,
            domainId = item.domainId?.ifBlank { null } ?: "kr",
            domain = null,
            bannerNm = item.bannerName,
            linkUrl = item.linkUrl,
            bannerImage = item.imagePath,
            bannerImageFile = item.imageFileName,
            bannerDc = item.description,
            sortOrdr = item.sortOrder,
            reflctAt = item.activeYn ?: "Y",
            frstRegisterId = item.createdBy,
            frstRegistPnttm = java.time.LocalDateTime.now(),
            lastUpdusrId = null,
            lastUpdtPnttm = null,
        )
        jpaRepository.save(e)
    }

    override fun update(item: BnrItem) {
        val e = jpaRepository.findById(item.bannerId).orElseThrow()
        e.domainId = item.domainId?.ifBlank { null } ?: "kr"
        e.bannerNm = item.bannerName
        e.linkUrl = item.linkUrl
        e.bannerImage = item.imagePath
        e.bannerImageFile = item.imageFileName
        e.bannerDc = item.description
        e.sortOrdr = item.sortOrder
        e.reflctAt = item.activeYn ?: "Y"
        e.lastUpdusrId = item.createdBy
        e.lastUpdtPnttm = java.time.LocalDateTime.now()
        jpaRepository.save(e)
    }

    override fun delete(bannerId: String) {
        jpaRepository.deleteById(bannerId)
    }

    private fun adminListPredicate(c: BnrListCriteria, root: Root<BnrEntity>, cb: CriteriaBuilder): Predicate {
        val domainId = c.domainId.ifBlank { "kr" }
        val keyword = c.keyword
        val searchType = c.searchType
        val domainPred = cb.equal(root.get<String>("domainId"), domainId)
        return if (searchType == "name" && !keyword.isNullOrBlank()) {
            cb.and(domainPred, cb.like(root.get("bannerNm"), "%$keyword%"))
        } else {
            domainPred
        }
    }

    private fun countSpec(c: BnrListCriteria): Specification<BnrEntity> =
        Specification { root, _, cb ->
            adminListPredicate(c, root, cb)
        }

    private fun domainIdEquals(domainId: String): Specification<BnrEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<String>("domainId"), domainId.ifBlank { "kr" })
        }

    private fun activeEquals(): Specification<BnrEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<String>("reflctAt"), "Y")
        }

    private fun toItem(e: BnrEntity): BnrItem =
        BnrItem(
            bannerId = e.bannerId,
            domainId = e.domainId,
            bannerName = e.bannerNm,
            linkUrl = e.linkUrl,
            imagePath = e.bannerImage,
            imageFileName = e.bannerImageFile,
            description = e.bannerDc,
            sortOrder = e.sortOrdr ?: 0,
            activeYn = e.reflctAt,
            createdBy = e.frstRegisterId,
            createdAtFormatted = e.frstRegistPnttm?.format(fmt),
            domainName = e.domain?.domainNm,
        )
}
