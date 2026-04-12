package com.sleekydz86.idolglow.pup.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface PupJpaRepository : JpaRepository<PupEntity, String>, JpaSpecificationExecutor<PupEntity>
