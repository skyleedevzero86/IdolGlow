package com.sleekydz86.idolglow.productpackage.option.infrastructure

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.option.domain.OptionRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Repository

@Repository
class OptionRepositoryImpl(
    private val optionJpaRepository: OptionJpaRepository
) : OptionRepository {

    override fun findAllByIdIn(optionIds: List<Long>): List<Option> {
        return optionJpaRepository.findAllByIdIn(optionIds)
    }
    override fun findAll(): List<Option> = optionJpaRepository.findAll()
    override fun findById(optionId: Long): Option? = optionJpaRepository.findByIdOrNull(optionId)
    override fun save(option: Option): Option = optionJpaRepository.save(option)
    override fun delete(option: Option) = optionJpaRepository.delete(option)
}
