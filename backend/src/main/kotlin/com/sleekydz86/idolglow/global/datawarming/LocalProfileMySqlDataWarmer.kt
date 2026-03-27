package com.sleekydz86.idolglow.global.datawarming

import com.sleekydz86.idolglow.global.security.JwtProvider
import com.sleekydz86.idolglow.image.domain.Image
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.image.infrastructure.persistence.ImageJpaRepository
import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.option.infrastructure.OptionJpaRepository
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductLocationPayload
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import com.sleekydz86.idolglow.productpackage.product.infrastructure.ProductJpaRepository
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.infrastructure.ReservationJpaRepository
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.infrastructure.ProductReviewJpaRepository
import com.sleekydz86.idolglow.schedule.domain.Schedule
import com.sleekydz86.idolglow.schedule.infrastructure.ScheduleJpaRepository
import com.sleekydz86.idolglow.user.auth.domain.UserOAuth
import com.sleekydz86.idolglow.user.auth.domain.vo.AuthProvider
import com.sleekydz86.idolglow.user.auth.infrastructure.UserOAuthJpaRepository
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserSurvey
import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import com.sleekydz86.idolglow.user.user.infrastructure.UserSurveyJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

@Profile("local", "dev")
@Component
class LocalProfileMySqlDataWarmer(
    private val userJpaRepository: UserJpaRepository,
    private val userOAuthJpaRepository: UserOAuthJpaRepository,
    private val userSurveyJpaRepository: UserSurveyJpaRepository,
    private val optionJpaRepository: OptionJpaRepository,
    private val productJpaRepository: ProductJpaRepository,
    private val reservationJpaRepository: ReservationJpaRepository,
    private val scheduleJpaRepository: ScheduleJpaRepository,
    private val productReviewJpaRepository: ProductReviewJpaRepository,
    private val imageJpaRepository: ImageJpaRepository,
    private val jwtProvider: JwtProvider
) : ApplicationRunner {

    private val log = LoggerFactory.getLogger(LocalProfileMySqlDataWarmer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments) {
        val existingUser = userJpaRepository.findById(1L).orElse(null)
        if (existingUser != null) {
            logTokenForUser(existingUser)
        }

        if (userJpaRepository.count() > 0) {
            return
        }

        val user1 = userJpaRepository.save(
            User.Companion.of(
                email = "alice@example.com",
                nickname = "alice01",
                role = UserRole.ADMIN
            )
        )
        val user2 = userJpaRepository.save(User.Companion.of(email = "bob@example.com", nickname = "bob02"))

        userOAuthJpaRepository.save(
            UserOAuth.Companion.of(
                userId = user1.id,
                provider = AuthProvider.TEST,
                providerId = "google",
                email = user1.email
            )
        )

        userSurveyJpaRepository.save(
            UserSurvey.of(
                user = user1,
                concept = ConceptType.DREAMY,
                idolName = "Sample Idol name~",
                visitStartDate = LocalDate.now().plusDays(3),
                visitEndDate = LocalDate.now().plusDays(5),
                places = listOf("Seoul", "Hongdae")
            )
        )

        val option1 = optionJpaRepository.save(
            Option(
                name = "Photo Shoot",
                description = "Studio photo shoot option.",
                price = BigDecimal("5000.00"),
                location = "Seoul Studio"
            )
        )
        val option2 = optionJpaRepository.save(
            Option(
                name = "Makeup Session",
                description = "Professional makeup session.",
                price = BigDecimal("8000.00"),
                location = "Seoul Salon"
            )
        )

        val slotStartDate = LocalDate.now().plusDays(1)
        val slotEndDate = slotStartDate.plusDays(1)
        val product = Product.Companion.createWithTimeSlots(
            name = "Idol Experience",
            description = "One-day idol experience with photos and styling.",
            options = listOf(option1, option2),
            tagNames = listOf("idol", "photo", "makeup"),
            slotStartDate = slotStartDate,
            slotEndDate = slotEndDate
        )
        val locationPayload = ProductLocationPayload(
            name = "Doki Studio",
            latitude = BigDecimal("37.5665"),
            longitude = BigDecimal("126.9780"),
            roadAddressName = "1 Seoul-ro",
            addressName = "Seoul, Korea",
            kakaoPlaceId = "kakao-1234"
        )
        val productLocation = ProductLocation.Companion.of(product, locationPayload)
        product.setLocation(productLocation)
        val savedProduct = productJpaRepository.save(product)

        val reservationSlot = savedProduct.reservationSlots.first()
        val reservationExpiresAt = LocalDateTime.now().plusMinutes(15)
        val reservation = Reservation(
            reservationSlot = reservationSlot,
            userId = user1.id,
            visitDate = reservationSlot.reservationDate,
            visitStartTime = reservationSlot.startTime,
            visitEndTime = reservationSlot.endTime,
            totalPrice = savedProduct.totalPrice
        ).request(reservationExpiresAt)
        val savedReservation = reservationJpaRepository.save(reservation)
        reservationSlot.hold(savedReservation.id, reservationExpiresAt)
        savedReservation.confirm()

        val scheduleStart = LocalDateTime.of(reservationSlot.reservationDate, reservationSlot.startTime)
        val scheduleEnd = LocalDateTime.of(reservationSlot.reservationDate, reservationSlot.endTime)
        scheduleJpaRepository.save(
            Schedule.of(
                userId = user1.id,
                productId = savedProduct.id,
                title = "Local sample visit",
                startAt = scheduleStart,
                endAt = scheduleEnd
            )
        )

        val review = productReviewJpaRepository.save(
            ProductReview.of(
                product = savedProduct,
                userId = user1.id,
                ratingScore = 5,
                content = "So bad..."
            )
        )

        logTokenForUser(user1)

        imageJpaRepository.saveAll(
            listOf(
                Image(
                    aggregateType = ImageAggregateType.PRODUCT,
                    aggregateId = savedProduct.id,
                    originalFilename = "product.jpg",
                    uniqueFilename = "product-1.jpg",
                    extension = "jpg",
                    fileSize = 12345,
                    url = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSvgc55oqyCgDe0etT2gGO0-TVO4aTJJo2ZCMXSIjb48AjGSchKAsofP_ExwitaeDfhSMU85Zongk3oLNBfgnFhLaG8qBKDF2IAA6hq3Q&s=10",
                    sortOrder = 0
                ),
                Image(
                    aggregateType = ImageAggregateType.OPTION,
                    aggregateId = option1.id,
                    originalFilename = "option.jpg",
                    uniqueFilename = "option-1.jpg",
                    extension = "jpg",
                    fileSize = 23456,
                    url = "https://encrypted-tbn0.gstatic.com/images?q=tbn:ANd9GcSvgc55oqyCgDe0etT2gGO0-TVO4aTJJo2ZCMXSIjb48AjGSchKAsofP_ExwitaeDfhSMU85Zongk3oLNBfgnFhLaG8qBKDF2IAA6hq3Q&s=10",
                    sortOrder = 0
                ),
                Image(
                    aggregateType = ImageAggregateType.USER,
                    aggregateId = user2.id,
                    originalFilename = "user.jpg",
                    uniqueFilename = "user-1.jpg",
                    extension = "jpg",
                    fileSize = 34567,
                    url = "https://i.namu.wiki/i/16EvgURhGL60lzie4kEN-z_tVzwlMsvcf_kPlY3aShL4kfg9Jng2QvB_72EJGFn2l2aNHBl8FjJLxZmRGaNqFQ.webp",
                    sortOrder = 0
                ),
                Image(
                    aggregateType = ImageAggregateType.PRODUCT_REVIEW,
                    aggregateId = review.id,
                    originalFilename = "review.jpg",
                    uniqueFilename = "review-1.jpg",
                    extension = "jpg",
                    fileSize = 45678,
                    url = "https://img4.daumcdn.net/thumb/R658x0.q70/?fname=https://t1.daumcdn.net/news/202504/30/xportsnews/20250430200826062kfst.jpg",
                    sortOrder = 0
                )
            )
        )
    }

    private fun logTokenForUser(user: User) {
        val token = jwtProvider.generateToken(user.id, user.role)
        log.info(
            "로컬 테스트 토큰(userId={}): 액세스 토큰={}, 리프레시 토큰={}",
            user.id,
            token.accessToken,
            token.refreshToken
        )
    }
}
