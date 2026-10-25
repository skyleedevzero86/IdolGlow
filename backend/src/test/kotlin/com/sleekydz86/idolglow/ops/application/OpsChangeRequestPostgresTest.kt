package com.sleekydz86.idolglow.ops.application

import com.sleekydz86.idolglow.eventinfo.application.EventMatchMergeHandler
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.ops.domain.OpsActorKind
import com.sleekydz86.idolglow.ops.domain.OpsChangeStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
import java.util.UUID

class OpsChangeRequestPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var opsChangeCommandService: OpsChangeCommandService

    @Autowired
    private lateinit var eventMasterPort: EventMasterPort

    @Autowired
    private lateinit var eventSourceRefPort: EventSourceRefPort

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clearSecurity() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun userCannotApprove() {
        val operator = user(UserRole.OPERATOR)
        val member = user(UserRole.USER)
        val requestId = requestMerge(operator, conflictRef())

        authenticate(member)
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                opsChangeCommandService.approve(requestId)
            }

        assertEquals(true, error.message?.contains("운영 변경 권한이 없습니다") == true)
        assertEquals(OpsChangeStatus.REQUESTED.name, statusOf(requestId))
    }

    @Test
    fun requesterCannotApproveOwnRequest() {
        val admin = user(UserRole.ADMIN)
        val requestId = requestMerge(admin, conflictRef())

        authenticate(admin)
        val error =
            assertThrows(IllegalArgumentException::class.java) {
                opsChangeCommandService.approve(requestId)
            }

        assertEquals(true, error.message?.contains("자기가 만든 요청은 승인할 수 없습니다") == true)
        assertEquals(OpsChangeStatus.REQUESTED.name, statusOf(requestId))
    }

    @Test
    fun secondApprovalAppliesOnce() {
        val operator = user(UserRole.OPERATOR)
        val approver = user(UserRole.APPROVER)
        val ref = conflictRef()
        val requestId = requestMerge(operator, ref)

        authenticate(approver)
        opsChangeCommandService.approve(requestId)
        val second =
            assertThrows(IllegalArgumentException::class.java) {
                opsChangeCommandService.approve(requestId)
            }
        authenticate(operator)
        opsChangeCommandService.apply(requestId)
        val secondApply =
            assertThrows(IllegalArgumentException::class.java) {
                opsChangeCommandService.apply(requestId)
            }

        assertEquals(true, second.message?.contains("승인할 수 없는 상태입니다") == true)
        assertEquals(true, secondApply.message?.contains("적용할 수 없는 상태입니다") == true)
        assertEquals(OpsChangeStatus.APPLIED.name, statusOf(requestId))
        assertEquals(ref.globalId, reloaded(ref).globalId)
        assertEquals(EventMatchStatus.CONFIRMED, reloaded(ref).matchStatus)
    }

    @Test
    fun applyThenRevertRestoresConflictMapping() {
        val operator = user(UserRole.OPERATOR)
        val approver = user(UserRole.APPROVER)
        val ref = conflictRef()
        val requestId = requestMerge(operator, ref)

        authenticate(approver)
        opsChangeCommandService.approve(requestId)
        authenticate(operator)
        opsChangeCommandService.apply(requestId)
        assertEquals(ref.globalId, reloaded(ref).globalId)
        opsChangeCommandService.revert(requestId)

        val restored = reloaded(ref)
        assertEquals(OpsChangeStatus.REVERTED.name, statusOf(requestId))
        assertNull(restored.globalId)
        assertEquals(EventMatchStatus.CONFLICT, restored.matchStatus)
        assertEquals(
            listOf("OPS_CHANGE_REQUEST", "OPS_CHANGE_APPROVE", "OPS_CHANGE_APPLY", "OPS_CHANGE_REVERT"),
            auditActions(requestId),
        )
    }

    private fun requestMerge(
        actor: User,
        ref: ConflictRef,
    ): Long {
        authenticate(actor)
        return opsChangeCommandService
            .request(
                OpsChangeCommand(
                    type = EventMatchMergeHandler.TYPE,
                    target = "${ref.source}|${ref.sourceId}",
                    payload = """{"globalId":"${ref.globalId}"}""",
                    kind = OpsActorKind.USER,
                    agentName = null,
                ),
            ).id
    }

    private fun conflictRef(): ConflictRef {
        val token = UUID.randomUUID().toString().take(8)
        val master =
            eventMasterPort.save(
                EventMaster.open(
                    CollectedPerformance(
                        source = "KOPIS_PBLPRFR",
                        sourceId = "MASTER-$token",
                        title = "호두까기 인형",
                        venue = "롯데콘서트홀",
                        startDate = LocalDate.of(2026, 1, 5),
                        endDate = LocalDate.of(2026, 2, 5),
                        rawPayload = "{}",
                    ),
                ),
            )
        eventSourceRefPort.save(
            EventSourceRef(
                source = "KOPIS_PBLPRFR",
                sourceId = "PF-$token",
                globalId = null,
                matchStatus = EventMatchStatus.CONFLICT,
                matchScore = 0,
                rawHash = "hash",
                title = "호두까기 인형",
            ),
        )
        return ConflictRef(source = "KOPIS_PBLPRFR", sourceId = "PF-$token", globalId = master.globalId)
    }

    private fun reloaded(ref: ConflictRef): EventSourceRef =
        eventSourceRefPort.findBySourceAndSourceId(ref.source, ref.sourceId)
            ?: throw IllegalStateException("공연 공급처 원본이 없습니다. sourceId=${ref.sourceId}")

    private fun user(role: UserRole): User {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        return userJpaRepository.save(User.of("ops$token@test.com", "u$token", role))
    }

    private fun authenticate(actor: User) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                actor.id.toString(),
                "",
                listOf(SimpleGrantedAuthority("ROLE_${actor.role.name}")),
            )
    }

    private fun statusOf(requestId: Long): String =
        jdbcTemplate.queryForObject(
            "select status from ops_change_request where id = ?",
            String::class.java,
            requestId,
        )!!

    private fun auditActions(requestId: Long): List<String> =
        jdbcTemplate.query(
            """
            select action_code from admin_audit_logs
            where target_type = 'OPS_CHANGE_REQUEST' and target_id = ?
            order by id
            """.trimIndent(),
            { rs, _ -> rs.getString("action_code") },
            requestId,
        )

    private data class ConflictRef(
        val source: String,
        val sourceId: String,
        val globalId: String,
    )
}
