package com.sleekydz86.idolglow.platform.user.infrastructure

import com.sleekydz86.idolglow.platform.user.applyToEntity
import com.sleekydz86.idolglow.platform.user.port.PlatformUserAccountPort
import com.sleekydz86.idolglow.platform.user.domain.PlatformUser
import com.sleekydz86.idolglow.platform.user.toPlatformUser
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.Optional

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformUserAccountAdapter(
    private val userJpaRepository: UserJpaRepository,
    private val userRepository: UserRepository,
) : PlatformUserAccountPort {

    override fun save(platformUser: PlatformUser): PlatformUser {
        val entity = platformUser.id?.takeIf { it > 0 }?.let { userJpaRepository.findById(it).orElse(null) }
            ?: userRepository.findByEmail(platformUser.email)
            ?: User(
                email = platformUser.email,
                nickname = Nickname.of(
                    platformUser.nickname.ifBlank { platformUser.username },
                ),
                profileImageUrl = null,
                passwordHash = "",
                role = com.sleekydz86.idolglow.user.user.domain.vo.UserRole.USER,
            )
        platformUser.applyToEntity(entity)
        return userRepository.save(entity).toPlatformUser()
    }

    override fun findById(id: Long): Optional<PlatformUser> =
        Optional.ofNullable(userRepository.findById(id)?.toPlatformUser())

    override fun findByEmail(email: String): Optional<PlatformUser> =
        Optional.ofNullable(userRepository.findByEmail(email)?.toPlatformUser())

    override fun existsByEmail(email: String): Boolean =
        userRepository.findByEmail(email) != null

    override fun findByEmailAndUsername(email: String, username: String): Optional<PlatformUser> {
        val user = userJpaRepository.findByEmailAndPlatformUsername(email, username)
            ?: userJpaRepository.findByEmailAndNicknameValue(email, username)
        return Optional.ofNullable(user?.toPlatformUser())
    }
}
