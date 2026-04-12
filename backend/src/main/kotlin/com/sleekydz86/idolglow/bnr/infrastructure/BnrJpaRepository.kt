package com.sleekydz86.idolglow.bnr.infrastructure

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface BnrJpaRepository : JpaRepository<BnrEntity, String>, JpaSpecificationExecutor<BnrEntity>
