package com.sleekydz86.idolglow.mim.infrastructure

import com.sleekydz86.idolglow.mim.domain.MimItem
import com.sleekydz86.idolglow.mim.domain.MimListCriteria
import com.sleekydz86.idolglow.mim.domain.MimRepository
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
class MimRepositoryImpl(
    private val jpaRepository: MimJpaRepository,
    private val entityManager: EntityManager,
) : MimRepository {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findList(criteria: MimListCriteria): List<MimItem> {
        val pageable = PageRequest.of(
            criteria.pageIndex - 1,
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "frstRegistPnttm"),
        )
        val cb = entityManager.criteriaBuilder
        val cq = cb.createQuery(MimEntity::class.java)
        val root = cq.from(MimEntity::class.java)
        root.fetch<MimEntity, SdomEntity>("domain", JoinType.LEFT)
        cq.select(root).distinct(true)
        cq.where(adminListPredicate(criteria, root, cb))
        cq.orderBy(cb.desc(root.get<LocalDateTime>("frstRegistPnttm")))
        val q = entityManager.createQuery(cq)
        q.firstResult = pageable.offset.toInt()
        q.maxResults = pageable.pageSize
        return q.resultList.map(::toItem)
    }

    override fun count(criteria: MimListCriteria): Int =
        jpaRepository.count(countSpec(criteria)).toInt()

    override fun findById(imageId: String): MimItem? =
        jpaRepository.findById(imageId).map { toItem(it) }.orElse(null)

    override fun findActiveByDomain(domainId: String): List<MimItem> =
        jpaRepository.findAll(
            Specification
                .where(domainIdEquals(domainId))
                .and(activeEquals()),
            Sort.by(Sort.Order.asc("frstRegistPnttm")),
        ).map(::toItem)

    override fun insert(item: MimItem) {
        val e = MimEntity(
            imageId = item.imageId,
            domainId = item.domainId?.ifBlank { null } ?: "kr",
            domain = null,
            imageNm = item.imageName,
            image = item.imagePath,
            imageFile = item.imageFileName,
            imageDc = item.description,
            reflctAt = item.activeYn ?: "Y",
            frstRegisterId = item.createdBy,
            frstRegistPnttm = java.time.LocalDateTime.now(),
            lastUpdusrId = null,
            lastUpdtPnttm = null,
        )
        jpaRepository.save(e)
    }

    override fun update(item: MimItem) {
        val e = jpaRepository.findById(item.imageId).orElseThrow()
        e.domainId = item.domainId?.ifBlank { null } ?: "kr"
        e.imageNm = item.imageName
        e.image = item.imagePath
        e.imageFile = item.imageFileName
        e.imageDc = item.description
        e.reflctAt = item.activeYn ?: "Y"
        e.lastUpdusrId = item.createdBy
        e.lastUpdtPnttm = java.time.LocalDateTime.now()
        jpaRepository.save(e)
    }

    override fun delete(imageId: String) {
        jpaRepository.deleteById(imageId)
    }

    private fun adminListPredicate(c: MimListCriteria, root: Root<MimEntity>, cb: CriteriaBuilder): Predicate {
        val domainId = c.domainId.ifBlank { "kr" }
        val domainPred = cb.equal(root.get<String>("domainId"), domainId)
        val keyword = c.keyword.trim().lowercase()
        if (keyword.isBlank()) {
            return domainPred
        }

        val pattern = "%$keyword%"
        val conditions = when (c.searchType) {
            "name" -> listOf(cb.like(cb.lower(root.get("imageNm")), pattern))
            "description" -> listOf(cb.like(cb.lower(root.get("imageDc")), pattern))
            else -> listOf(
                cb.like(cb.lower(root.get("imageNm")), pattern),
                cb.like(cb.lower(root.get("imageDc")), pattern),
                cb.like(cb.lower(root.get("imageFile")), pattern),
                cb.like(cb.lower(root.get("image")), pattern),
            )
        }
        return cb.and(domainPred, cb.or(*conditions.toTypedArray()))
    }

    private fun countSpec(c: MimListCriteria): Specification<MimEntity> =
        Specification { root, _, cb ->
            adminListPredicate(c, root, cb)
        }

    private fun domainIdEquals(domainId: String): Specification<MimEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<String>("domainId"), domainId.ifBlank { "kr" })
        }

    private fun activeEquals(): Specification<MimEntity> =
        Specification { root, _, cb ->
            cb.equal(root.get<String>("reflctAt"), "Y")
        }

    private fun toItem(e: MimEntity): MimItem =
        MimItem(
            imageId = e.imageId,
            domainId = e.domainId,
            imageName = e.imageNm,
            imagePath = e.image,
            imageFileName = e.imageFile,
            description = e.imageDc,
            activeYn = e.reflctAt,
            createdBy = e.frstRegisterId,
            createdAtFormatted = e.frstRegistPnttm?.format(fmt),
            domainName = e.domain?.domainNm,
        )
}
