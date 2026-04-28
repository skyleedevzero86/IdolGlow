package com.sleekydz86.idolglow.exchange.application

import com.sleekydz86.idolglow.exchange.adapter.web.dto.CreateExchangeAlertRequest
import com.sleekydz86.idolglow.exchange.domain.ExchangeAlert
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeAlertJpaRepository
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ExchangeAlertCommandService(
    private val userRepository: UserRepository,
    private val exchangeAlertJpaRepository: ExchangeAlertJpaRepository,
) {

    @Transactional
    fun create(userId: Long, request: CreateExchangeAlertRequest): Long {
        val user = userRepository.findById(userId)
            ?: throw IllegalArgumentException("ID가 $userId 인 사용자를 찾을 수 없습니다.")
        val from = request.fromCurrency.trim().uppercase().substringBefore('(').trim()
        val to = request.toCurrency.trim().uppercase().substringBefore('(').trim()
        val saved = exchangeAlertJpaRepository.save(
            ExchangeAlert(
                user = user,
                fromCurrency = from,
                toCurrency = to,
                targetRate = request.targetRate.stripTrailingZeros(),
            ),
        )
        return saved.id
    }
}
