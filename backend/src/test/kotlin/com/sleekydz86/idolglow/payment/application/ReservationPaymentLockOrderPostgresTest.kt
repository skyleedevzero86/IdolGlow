package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.CannotAcquireLockException
import org.springframework.dao.DeadlockLoserDataAccessException
import org.springframework.dao.PessimisticLockingFailureException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@DisplayName("예약·결제 잠금 순서 PostgreSQL")
class ReservationPaymentLockOrderPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var reservationCommandService: ReservationCommandService

    @Autowired
    private lateinit var reservationPaymentService: ReservationPaymentService

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @DisplayName("같은 예약에 취소와 결제 성공을 동시에 여러 번 실행해도 데드락이 나지 않는다")
    @Test
    fun cancelAndSuccessDoNotDeadlock() {
        val lockFailures = AtomicInteger()
        val pool = Executors.newFixedThreadPool(4)
        repeat(30) {
            val user = fixture.persistUser()
            val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
            val start = CountDownLatch(1)
            val done = CountDownLatch(2)
            pool.submit {
                start.await()
                runCatching {
                    reservationCommandService.cancelReservationByUser(created.id, user.id)
                }.onFailure { error -> if (isLockFailure(error)) lockFailures.incrementAndGet() }
                done.countDown()
            }
            pool.submit {
                start.await()
                runCatching {
                    reservationPaymentService.handlePaymentSucceeded(created.payment.paymentReference)
                }.onFailure { error -> if (isLockFailure(error)) lockFailures.incrementAndGet() }
                done.countDown()
            }
            start.countDown()
            done.await(20, TimeUnit.SECONDS)
        }
        pool.shutdownNow()
        assertEquals(0, lockFailures.get())
    }

    @DisplayName("결제 FOR UPDATE 는 예약 행을 같이 잠그지 않는다")
    @Test
    fun paymentForUpdateDoesNotLockReservationRow() {
        val created = fixture.createReservation(fixture.persistUser().id, fixture.persistOpenSlot())
        val lockedTables =
            TransactionTemplate(transactionManager).execute { status ->
                status.setRollbackOnly()
                paymentJpaRepository.findByOrderIdForUpdate(created.payment.orderId)
                jdbcTemplate.queryForList(
                    """
                    select c.relname
                    from pg_locks l
                    join pg_class c on c.oid = l.relation
                    join pg_namespace n on n.oid = c.relnamespace
                    where l.pid = pg_backend_pid()
                      and n.nspname = 'public'
                      and l.locktype = 'relation'
                      and l.mode in (
                        'RowShareLock',
                        'RowExclusiveLock',
                        'ShareLock',
                        'ShareRowExclusiveLock',
                        'ExclusiveLock',
                        'AccessExclusiveLock'
                      )
                    """.trimIndent(),
                    String::class.java,
                )
            }!!
        assertFalse(lockedTables.contains("reservations"), lockedTables.toString())
    }

    private fun isLockFailure(error: Throwable): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is PessimisticLockingFailureException ||
                current is CannotAcquireLockException ||
                current is DeadlockLoserDataAccessException
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }
}
