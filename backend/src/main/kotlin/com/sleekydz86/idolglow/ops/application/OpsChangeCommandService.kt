package com.sleekydz86.idolglow.ops.application

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.ops.application.port.OpsChangeHandler
import com.sleekydz86.idolglow.ops.application.port.OpsChangeRequestPort
import com.sleekydz86.idolglow.ops.domain.OpsActorKind
import com.sleekydz86.idolglow.ops.domain.OpsChangeRequest
import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class OpsChangeCommandService(
    private val opsChangeRequestPort: OpsChangeRequestPort,
    private val handlers: List<OpsChangeHandler>,
    private val adminAuditService: AdminAuditService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {
    @Transactional
    fun request(command: OpsChangeCommand): OpsChangeRequest {
        val actor = requireActor(UserRole.OPERATOR, UserRole.ADMIN)
        val requestedBy = requestedBy(command, actor.userId)
        handler(command.type).validate(command.target, command.payload)
        val saved =
            opsChangeRequestPort.save(
                OpsChangeRequest.open(command.type, command.target, command.payload, requestedBy, command.kind),
            )
        audit(ACTION_REQUEST, saved)
        return saved
    }

    @Transactional
    fun approve(requestId: Long): OpsChangeRequest {
        val actor = requireActor(UserRole.APPROVER, UserRole.ADMIN)
        val request = locked(requestId)
        request.approve(actor.userId)
        val saved = opsChangeRequestPort.save(request)
        audit(ACTION_APPROVE, saved)
        return saved
    }

    @Transactional
    fun reject(requestId: Long): OpsChangeRequest {
        requireActor(UserRole.APPROVER, UserRole.ADMIN)
        val request = locked(requestId)
        request.reject()
        val saved = opsChangeRequestPort.save(request)
        audit(ACTION_REJECT, saved)
        return saved
    }

    @Transactional
    fun apply(requestId: Long): OpsChangeRequest {
        requireActor(UserRole.OPERATOR, UserRole.ADMIN)
        val request = locked(requestId)
        val change = handler(request.type)
        request.markApplied(change.snapshot(request.target), LocalDateTime.now())
        change.apply(request.target, request.payload)
        val saved = opsChangeRequestPort.save(request)
        audit(ACTION_APPLY, saved)
        return saved
    }

    @Transactional
    fun revert(requestId: Long): OpsChangeRequest {
        requireActor(UserRole.OPERATOR, UserRole.ADMIN)
        val request = locked(requestId)
        request.markReverted()
        handler(request.type).revert(request.target, request.beforeSnapshot.orEmpty())
        val saved = opsChangeRequestPort.save(request)
        audit(ACTION_REVERT, saved)
        return saved
    }

    @Transactional(readOnly = true)
    fun findRecent(): List<OpsChangeRequest> {
        requireActor(UserRole.OPERATOR, UserRole.APPROVER, UserRole.ADMIN)
        return opsChangeRequestPort.findRecent()
    }

    private fun requestedBy(
        command: OpsChangeCommand,
        actorId: Long,
    ): String =
        if (command.kind == OpsActorKind.USER) {
            actorId.toString()
        } else {
            command.agentName?.trim().orEmpty().also { name ->
                require(name.isNotEmpty()) { "에이전트 이름은 비울 수 없습니다. type=${command.type}" }
            }
        }

    private fun locked(requestId: Long): OpsChangeRequest =
        opsChangeRequestPort.findByIdForUpdate(requestId)
            ?: throw IllegalArgumentException("운영 변경 요청을 찾을 수 없습니다. requestId=$requestId")

    private fun handler(type: String): OpsChangeHandler =
        handlers.find { it.type == type.trim() }
            ?: throw IllegalArgumentException("지원하지 않는 운영 변경입니다. type=$type")

    private fun audit(
        actionCode: String,
        request: OpsChangeRequest,
    ) {
        adminAuditService.log(
            actionCode = actionCode,
            targetType = TARGET_TYPE,
            targetId = request.id,
            detail = "status=${request.status} type=${request.type} target=${request.target}",
        )
    }

    private fun requireActor(vararg allowed: UserRole): OpsActor {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val roles =
            SecurityContextHolder
                .getContext()
                .authentication
                ?.authorities
                ?.mapNotNull { it.authority?.removePrefix("ROLE_") }
                .orEmpty()
        val role =
            allowed.firstOrNull { it.name in roles }
                ?: throw IllegalArgumentException("운영 변경 권한이 없습니다. role=${roles.joinToString(",")}")
        return OpsActor(userId, role)
    }

    private data class OpsActor(
        val userId: Long,
        val role: UserRole,
    )

    companion object {
        const val TARGET_TYPE: String = "OPS_CHANGE_REQUEST"
        const val ACTION_REQUEST: String = "OPS_CHANGE_REQUEST"
        const val ACTION_APPROVE: String = "OPS_CHANGE_APPROVE"
        const val ACTION_REJECT: String = "OPS_CHANGE_REJECT"
        const val ACTION_APPLY: String = "OPS_CHANGE_APPLY"
        const val ACTION_REVERT: String = "OPS_CHANGE_REVERT"
    }
}
