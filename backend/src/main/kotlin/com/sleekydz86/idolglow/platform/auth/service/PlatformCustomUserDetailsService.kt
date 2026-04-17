package com.sleekydz86.idolglow.platform.auth.service

import com.sleekydz86.idolglow.platform.user.port.PlatformUserAccountPort
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformCustomUserDetailsService(
    private val userAccountPort: PlatformUserAccountPort,
) : UserDetailsService {

    private val log = LoggerFactory.getLogger(PlatformCustomUserDetailsService::class.java)

    @Transactional(readOnly = true)
    override fun loadUserByUsername(email: String): UserDetails {
        log.debug("사용자 정보 로드: {}", email)

        val user = userAccountPort.findByEmail(email)
            .orElseThrow { UsernameNotFoundException("사용자를 찾을 수 없습니다: $email") }

        if (!user.isActive()) {
            throw UsernameNotFoundException("비활성 사용자입니다: $email")
        }

        return org.springframework.security.core.userdetails.User.builder()
            .username(user.email)
            .password(user.password)
            .authorities(listOf(SimpleGrantedAuthority("ROLE_${user.role.name}")))
            .disabled(!user.isActive())
            .build()
    }
}
