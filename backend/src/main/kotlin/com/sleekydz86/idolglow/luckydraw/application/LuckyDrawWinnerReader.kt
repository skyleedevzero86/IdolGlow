package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.luckydraw.application.dto.LuckyDrawAdminWinner
import com.sleekydz86.idolglow.luckydraw.domain.NicknameMask
import com.sleekydz86.idolglow.luckydraw.infrastructure.LuckyDrawWinnerJpaRepository
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import org.springframework.stereotype.Component

@Component
class LuckyDrawWinnerReader(
    private val luckyDrawWinnerJpaRepository: LuckyDrawWinnerJpaRepository,
    private val userRepository: UserRepository,
) {
    fun adminWinners(drawId: Long): List<LuckyDrawAdminWinner> =
        luckyDrawWinnerJpaRepository.findAllByLuckyDrawIdOrderByWinnerOrderAsc(drawId).map { winner ->
            LuckyDrawAdminWinner(winner.userId, nicknameOf(winner.userId), winner.winnerOrder)
        }

    fun maskedWinners(drawId: Long): List<String> = adminWinners(drawId).map { NicknameMask.mask(it.nickname) }

    private fun nicknameOf(userId: Long): String = userRepository.findById(userId)?.nickname?.value ?: ""
}
