package com.sleekydz86.idolglow.global.infrastructure.datawarming

import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Profile("local")
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
class LocalSeedUserPasswordInitializer(
    private val userJpaRepository: UserJpaRepository,
    private val passwordEncoder: PasswordEncoder,
    @Value("\${LOCAL_SEED_USER_PASSWORD:}")
    private val seedUserPassword: String,
) : ApplicationRunner {
    private val log = LoggerFactory.getLogger(LocalSeedUserPasswordInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        if (seedUserPassword.isBlank()) return
        val user = userJpaRepository.findById(SEED_USER_ID).orElse(null)
        when {
            user == null -> log.warn("로컬 테스트 사용자가 없어 비밀번호를 설정하지 않았습니다. userId={}", SEED_USER_ID)
            isAlreadySet(user) -> Unit
            else -> applyPassword(user)
        }
    }

    private fun isAlreadySet(user: User): Boolean =
        user.passwordHash?.let { passwordEncoder.matches(seedUserPassword, it) } == true

    private fun applyPassword(user: User) {
        val encoded = requireNotNull(passwordEncoder.encode(seedUserPassword)) { "비밀번호 인코딩에 실패했습니다." }
        user.applyEncodedPasswordChange(encoded)
        userJpaRepository.save(user)
        log.info("로컬 테스트 사용자 비밀번호를 환경 변수 값으로 설정했습니다. userId={}", SEED_USER_ID)
    }

    companion object {
        private const val SEED_USER_ID: Long = 1L
    }
}
