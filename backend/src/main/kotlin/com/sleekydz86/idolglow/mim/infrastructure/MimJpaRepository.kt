package com.sleekydz86.idolglow.mim.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface MimJpaRepository : JpaRepository<MimEntity, String>, JpaSpecificationExecutor<MimEntity>
