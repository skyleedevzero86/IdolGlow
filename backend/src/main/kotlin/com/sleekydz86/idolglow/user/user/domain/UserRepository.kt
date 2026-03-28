package com.sleekydz86.idolglow.user.user.domain

interface UserRepository {
    fun findById(userId: Long): User?
    fun findByEmail(email: String): User?
    fun save(user: User): User
    fun saveAndFlush(user: User): User
}