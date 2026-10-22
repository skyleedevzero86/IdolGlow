package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.application.ReservationCommandService
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.support.PostgresReservationFixture
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.util.concurrent.atomic.AtomicReference

@DisplayName("예약·결제 잠금 순서 PostgreSQL")
class ReservationPaymentLockOrderPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var fixture: PostgresReservationFixture

    @Autowired
    private lateinit var reservationCommandService: ReservationCommandService

    @Autowired
    private lateinit var reservationPaymentService: ReservationPaymentService

    @Autowired
    private lateinit var myPagePaymentService: MyPagePaymentService

    @Autowired
    private lateinit var paymentJpaRepository: PaymentJpaRepository

    @Autowired
    private lateinit var paymentRepository: PaymentRepository

    @Autowired
    private lateinit var reservationRepository: ReservationRepository

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

    @DisplayName("마이페이지 취소와 결제 성공을 동시에 여러 번 실행해도 데드락이 나지 않는다")
    @Test
    fun myPageCancelFollowsReservationThenPaymentLockOrder() {
        val lockFailures = AtomicInteger()
        val pool = Executors.newFixedThreadPool(4)
        try {
            repeat(30) {
                val user = fixture.persistUser()
                val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
                reservationPaymentService.handlePaymentSucceeded(created.payment.paymentReference)
                val start = CountDownLatch(1)
                val done = CountDownLatch(2)
                pool.submit {
                    start.await()
                    runCatching {
                        myPagePaymentService.cancelPayment(user.id, created.payment.paymentId)
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
                assertTrue(done.await(20, TimeUnit.SECONDS))
                assertConsistentCancelOrSuccess(created.payment.paymentId)
            }
            assertEquals(0, lockFailures.get())
        } finally {
            pool.shutdownNow()
        }
    }

    @DisplayName("잠금 대기 중 결제가 환불되면 마이페이지 취소는 거절된다")
    @Test
    fun myPageCancelSeesStateChangedBeforeLock() {
        val user = fixture.persistUser()
        val created = fixture.createReservation(user.id, fixture.persistOpenSlot())
        reservationPaymentService.handlePaymentSucceeded(created.payment.paymentReference)
        val holding = CountDownLatch(1)
        val release = CountDownLatch(1)
        val done = CountDownLatch(1)
        val failure = AtomicReference<Throwable>()
        val pool = Executors.newFixedThreadPool(2)
        try {
            pool.submit { holdRefundWhileLockingReservation(created.id, created.payment.paymentId, holding, release) }
            assertTrue(holding.await(20, TimeUnit.SECONDS))
            pool.submit {
                val thrown =
                    runCatching {
                        myPagePaymentService.cancelPayment(user.id, created.payment.paymentId)
                    }.exceptionOrNull()
                failure.set(thrown)
                done.countDown()
            }
            awaitReservationLockWaiter(done, failure)
            release.countDown()
            assertTrue(done.await(20, TimeUnit.SECONDS))
        } finally {
            release.countDown()
            pool.shutdownNow()
        }
        val thrown = failure.get()
        assertTrue(thrown is IllegalArgumentException, thrown?.toString() ?: "예외 없음")
        assertEquals("사용자 결제 취소 가능 기간(15일)이 지났거나 취소할 수 없는 상태입니다.", thrown?.message)
    }

    private fun holdRefundWhileLockingReservation(
        reservationId: Long,
        paymentId: Long,
        holding: CountDownLatch,
        release: CountDownLatch,
    ) {
        TransactionTemplate(transactionManager).executeWithoutResult {
            reservationRepository.findByIdForUpdate(reservationId)
            val payment =
                paymentRepository.findByIdForUpdate(paymentId)
                    ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
            payment.markFullRefund()
            holding.countDown()
            check(release.await(20, TimeUnit.SECONDS))
        }
    }

    private fun awaitReservationLockWaiter(
        done: CountDownLatch,
        failure: AtomicReference<Throwable>,
    ) {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(20)
        while (System.nanoTime() < deadline) {
            if (done.count == 0L) {
                throw IllegalStateException("마이페이지 취소가 잠금을 기다리지 않고 끝났습니다: ${failure.get()}")
            }
            val waiting =
                jdbcTemplate.queryForObject(
                    """
                    select count(*)
                    from pg_stat_activity
                    where wait_event_type = 'Lock'
                    """.trimIndent(),
                    Long::class.java,
                ) ?: 0L
            if (waiting > 0L) {
                return
            }
            Thread.sleep(50)
        }
        throw IllegalStateException("마이페이지 취소가 예약 잠금을 기다리지 않았습니다.")
    }

    private fun assertConsistentCancelOrSuccess(paymentId: Long) {
        val row =
            jdbcTemplate.queryForMap(
                """
                select p.status as payment_status, r.status as reservation_status
                from payments p
                join reservations r on r.id = p.reservation_id
                where p.id = ?
                """.trimIndent(),
                paymentId,
            )
        val paymentStatus = row["payment_status"].toString()
        val reservationStatus = row["reservation_status"].toString()
        val consistent =
            (paymentStatus == "SUCCEEDED" && reservationStatus == "BOOKED") ||
                (paymentStatus == "REFUNDED" && reservationStatus == "CANCELED")
        assertTrue(consistent, "payment=$paymentStatus reservation=$reservationStatus")
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
