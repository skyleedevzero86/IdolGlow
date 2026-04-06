package com.sleekydz86.idolglow.subscription.application.port.`in`

import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.dto.SubscriptionRegistrationResponse

interface SubscriptionPublicUseCase {
    fun subscribe(command: RegisterSubscriptionCommand): SubscriptionRegistrationResponse
}
