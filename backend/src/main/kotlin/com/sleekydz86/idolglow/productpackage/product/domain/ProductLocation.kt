package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductLocationPayload
import jakarta.persistence.*
import java.math.BigDecimal

@Entity
@Table(name = "product_locations")
class ProductLocation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    val product: Product,

    @Column(nullable = false, length = 120)
    var name: String,

    @Column(nullable = false, precision = 10, scale = 7)
    var latitude: BigDecimal,

    @Column(nullable = false, precision = 10, scale = 7)
    var longitude: BigDecimal,

    @Column(name = "road_address_name")
    var roadAddressName: String?,

    @Column(name = "address_name")
    var addressName: String?,

    @Column(name = "kakao_place_id", nullable = false, length = 40)
    var kakaoPlaceId: String
): BaseEntity() {

    init {
        validate()
    }

    fun displayAddress(): String = roadAddressName ?: addressName!!

    fun update(
        payload: ProductLocationPayload
    ) {
        name = payload.name
        latitude = payload.latitude
        longitude = payload.longitude
        roadAddressName = payload.roadAddressName
        addressName = payload.addressName
        kakaoPlaceId = payload.kakaoPlaceId
        validate()
    }

    private fun validate() {
        require(name.isNotBlank()) { "장소 이름은 비어 있을 수 없습니다." }
        require(latitude in LAT_MIN..LAT_MAX) { "위도는 -90 이상 90 이하여야 합니다." }
        require(longitude in LNG_MIN..LNG_MAX) { "경도는 -180 이상 180 이하여야 합니다." }
        require(!roadAddressName.isNullOrBlank() || !addressName.isNullOrBlank()) {
            "도로명 주소 또는 지번 주소 중 하나는 입력되어야 합니다."
        }
        require(kakaoPlaceId.isNotBlank()) { "카카오 장소 ID는 비어 있을 수 없습니다." }
    }

    companion object {
        private val LAT_MIN = BigDecimal("-90")
        private val LAT_MAX = BigDecimal("90")
        private val LNG_MIN = BigDecimal("-180")
        private val LNG_MAX = BigDecimal("180")

        fun of(
            product: Product,
            payload: ProductLocationPayload
        ): ProductLocation {
            return ProductLocation(
                product = product,
                name = payload.name,
                latitude = payload.latitude,
                longitude = payload.longitude,
                roadAddressName = payload.roadAddressName,
                addressName = payload.addressName,
                kakaoPlaceId = payload.kakaoPlaceId
            )
        }
    }
}
