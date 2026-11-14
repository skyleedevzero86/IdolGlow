package com.sleekydz86.idolglow.notification.application

import com.sleekydz86.idolglow.notification.application.event.AdminNotificationRequestedEvent
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.payment.application.PaymentAdminNotifications
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.math.BigDecimal
import java.util.UUID

@DisplayName("관리자 시스템 알림")
class AdminNotificationPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var adminNotificationPublisher: AdminNotificationPublisher

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @DisplayName("결제 완료 알림은 커밋 뒤 모든 관리자에게 가고 일반 회원에게는 가지 않는다")
    @Test
    fun paymentCompletedReachesEveryAdminOnly() {
        // given
        val admins = List(2) { member(UserRole.ADMIN) }
        val normal = member(UserRole.USER)
        val orderId = "ORD_${UUID.randomUUID()}"

        // when
        transactionTemplate.executeWithoutResult {
            adminNotificationPublisher.publish(PaymentAdminNotifications.completed(BigDecimal("35000"), orderId))
        }

        // then
        val adminCount =
            jdbcTemplate.queryForObject("select count(*) from users where role = 'ADMIN'", Int::class.java)!!
        assertEquals(adminCount, countFor(null, orderId))
        admins.forEach { assertEquals(1, countFor(it.id, orderId)) }
        assertEquals(0, countFor(normal.id, orderId))
        assertEquals(
            "35,000원 결제가 완료되었어요. (주문 $orderId)",
            jdbcTemplate.queryForObject(
                "select message from notifications where user_id = ? and message like ?",
                String::class.java,
                admins.first().id,
                "%$orderId%",
            ),
        )
    }

    @DisplayName("업무 트랜잭션이 롤백되면 관리자 알림을 만들지 않는다")
    @Test
    fun rollbackCreatesNothing() {
        // given
        val admin = member(UserRole.ADMIN)
        val marker = UUID.randomUUID().toString()

        // when
        transactionTemplate.executeWithoutResult { status ->
            adminNotificationPublisher.publish(event(marker))
            status.setRollbackOnly()
        }

        // then
        assertEquals(0, countFor(admin.id, marker))
    }

    @DisplayName("링크 중복 제거를 켜면 같은 알림은 관리자마다 한 번만 만든다")
    @Test
    fun deduplicatesByLink() {
        // given
        val admin = member(UserRole.ADMIN)
        val marker = UUID.randomUUID().toString()

        // when
        repeat(2) { adminNotificationPublisher.publish(event(marker, deduplicate = true)) }

        // then
        assertEquals(1, countFor(admin.id, marker))
    }

    private fun event(
        marker: String,
        deduplicate: Boolean = false,
    ) = AdminNotificationRequestedEvent(
        type = NotificationType.ADMIN_PAYMENT_ISSUE,
        title = "결제 확인 필요",
        message = "결제 확인이 필요한 건이 있어요: $marker",
        link = "/admin/payments#issue-$marker",
        deduplicateByLink = deduplicate,
    )

    private fun countFor(
        userId: Long?,
        marker: String,
    ): Int =
        if (userId == null) {
            jdbcTemplate.queryForObject(
                "select count(*) from notifications where message like ?",
                Int::class.java,
                "%$marker%",
            )!!
        } else {
            jdbcTemplate.queryForObject(
                "select count(*) from notifications where user_id = ? and message like ?",
                Int::class.java,
                userId,
                "%$marker%",
            )!!
        }

    private fun member(role: UserRole): User {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(8)
        val user = User(email = "an$suffix@test.com", nickname = Nickname.of("n$suffix"), role = role)
        return userJpaRepository.save(user)
    }
}
