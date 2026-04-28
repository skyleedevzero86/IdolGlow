package com.sleekydz86.idolglow.productpackage.option.infrastructure

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import jakarta.persistence.criteria.Path
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class OptionRepositoryImpl(
    private val optionJpaRepository: OptionJpaRepository,
) : OptionRepository {

    override fun findAllByIdIn(optionIds: List<Long>): List<Option> {
        return optionJpaRepository.findAllByIdIn(optionIds)
    }
    override fun findAll(): List<Option> = optionJpaRepository.findAll()
    override fun findById(optionId: Long): Option? = optionJpaRepository.findByIdOrNull(optionId)

    override fun findWithSearch(
        q: String?,
        pageable: Pageable,
    ): Page<Option> {
        if (q.isNullOrBlank()) {
            return optionJpaRepository.findAll(pageable)
        }
        val clean = q.trim()
        if (clean.isEmpty()) {
            return optionJpaRepository.findAll(pageable)
        }
        val like = "%" + clean.lowercase() + "%"
        val spec: Specification<Option> = Specification { root, _, cb ->
            val namePath: Path<String> = root.get("name")
            val locationPath: Path<String> = root.get("location")
            val descriptionPath: Path<String> = root.get("description")
            cb.or(
                cb.like(cb.lower(namePath), like),
                cb.like(cb.lower(locationPath), like),
                cb.like(cb.lower(descriptionPath), like),
            )
        }
        return optionJpaRepository.findAll(spec, pageable)
    }

    override fun save(option: Option): Option = optionJpaRepository.save(option)
    override fun delete(option: Option) = optionJpaRepository.delete(option)
}
