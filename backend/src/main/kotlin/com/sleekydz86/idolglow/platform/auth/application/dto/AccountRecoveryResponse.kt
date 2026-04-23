package com.sleekydz86.idolglow.platform.auth.application.dto

data class AccountRecoveryResponse(
    val success: Boolean,
    val message: String,
    val recoveryToken: String? = null,
    val expiresIn: Long = 0,
) {
    companion object {
        fun builder(): Builder = Builder()

        class Builder {
            private var success: Boolean = false
            private var message: String = ""
            private var recoveryToken: String? = null
            private var expiresIn: Long = 0

            fun success(success: Boolean) = apply { this.success = success }
            fun message(message: String) = apply { this.message = message }
            fun recoveryToken(recoveryToken: String?) = apply { this.recoveryToken = recoveryToken }
            fun expiresIn(expiresIn: Long) = apply { this.expiresIn = expiresIn }

            fun build(): AccountRecoveryResponse =
                AccountRecoveryResponse(success, message, recoveryToken, expiresIn)
        }
    }
}
