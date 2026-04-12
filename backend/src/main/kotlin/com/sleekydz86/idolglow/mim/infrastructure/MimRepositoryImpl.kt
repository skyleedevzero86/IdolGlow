package com.sleekydz86.idolglow.mim.infrastructure

import com.sleekydz86.idolglow.mim.domain.MimItem
import com.sleekydz86.idolglow.mim.domain.MimListCriteria
import com.sleekydz86.idolglow.mim.domain.MimRepository
import com.sleekydz86.idolglow.sdom.infrastructure.SdomEntity
import jakarta.persistence.criteria.JoinType
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Repository
import java.time.format.DateTimeFormatter

@Repository
class MimRepositoryImpl(
    private val jpaRepository: MimJpaRepository,
) : MimRepository {

    private val fmt: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

    override fun findList(criteria: MimListCriteria): List<MimItem> {
        val pageable = PageRequest.of(
            criteria.pageIndex - 1,
            criteria.pageSize,
            Sort.by(Sort.Direction.DESC, "frstRegistPnttm"),
        )
        return jpaRepository.findAll(specFrom(criteria), pageable).content.map { toItem(it) }
    }

    override fun count(criteria: MimListCriteria): Int =
        jpaRepository.count(specFrom(criteria)).toInt()

    override fun findById(imageId: String): MimItem? =
        jpaRepository.findById(imageId).map { toItem(it) }.orElse(null)

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
        e.imageNm = item.imageName
        e.imageDc = item.description
        e.reflctAt = item.activeYn ?: "Y"
        e.lastUpdusrId = item.createdBy
        e.lastUpdtPnttm = java.time.LocalDateTime.now()
        jpaRepository.save(e)
    }

    override fun delete(imageId: String) {
        jpaRepository.deleteById(imageId)
    }

    private fun specFrom(c: MimListCriteria): Specification<MimEntity> =
        Specification { root, query, cb ->
            query?.distinct(true)
            val isCountQuery = query?.resultType == Long::class.java ||
                query?.resultType == java.lang.Long::class.javaPrimitiveType
            if (!isCountQuery) {
                root.fetch<MimEntity, SdomEntity>("domain", JoinType.LEFT)
            }
            val domainId = c.domainId.ifBlank { "kr" }
            val keyword = c.keyword
            val searchType = c.searchType
            val domainPred = cb.equal(root.get<String>("domainId"), domainId)
            if (searchType == "name" && !keyword.isNullOrBlank()) {
                cb.and(domainPred, cb.like(root.get("imageNm"), "%$keyword%"))
            } else {
                domainPred
            }
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
