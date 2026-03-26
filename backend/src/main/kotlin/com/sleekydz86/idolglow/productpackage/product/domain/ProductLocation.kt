package com.sleekydz86.idolglow.productpackage.product.domain

import com.sleekydz86.idolglow.global.BaseEntity
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
        require(name.isNotBlank()) { "Location name must not be blank" }
        require(latitude in LAT_MIN..LAT_MAX) { "Latitude must be between -90 and 90" }
        require(longitude in LNG_MIN..LNG_MAX) { "Longitude must be between -180 and 180" }
        require(!roadAddressName.isNullOrBlank() || !addressName.isNullOrBlank()) {
            "Either road address or address must be provided"
        }
        require(kakaoPlaceId.isNotBlank()) { "kakaoPlaceId must not be blank" }
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
