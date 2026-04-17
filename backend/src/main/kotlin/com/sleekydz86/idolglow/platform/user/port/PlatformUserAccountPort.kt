package com.sleekydz86.idolglow.platform.user.port

import com.sleekydz86.idolglow.platform.user.domain.PlatformUser
import java.util.Optional

interface PlatformUserAccountPort {
    fun save(user: PlatformUser): PlatformUser
    fun findById(id: Long): Optional<PlatformUser>
    fun findByEmail(email: String): Optional<PlatformUser>
    fun existsByEmail(email: String): Boolean
    fun findByEmailAndUsername(email: String, username: String): Optional<PlatformUser>
}
