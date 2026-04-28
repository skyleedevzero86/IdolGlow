package com.sleekydz86.idolglow.exchange.infrastructure

import com.sleekydz86.idolglow.exchange.domain.ExchangeAlert
import org.springframework.data.jpa.repository.JpaRepository

interface ExchangeAlertJpaRepository : JpaRepository<ExchangeAlert, Long>
