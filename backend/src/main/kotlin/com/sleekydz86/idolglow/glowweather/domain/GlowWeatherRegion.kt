package com.sleekydz86.idolglow.glowweather.domain

data class GlowWeatherRegion(
    val id: String,
    val name: String,
    val areaLabel: String,
    val latitude: Double,
    val longitude: Double,
    val asosStationId: Int,
    val midForecastStationId: Int,
    val midLandRegionId: String,
    val midTemperatureRegionId: String,
)

object GlowWeatherRegions {
    val all: List<GlowWeatherRegion> = listOf(
        GlowWeatherRegion("seoul", "서울", "서울·경기", 37.5665, 126.9780, 108, 109, "11B00000", "11B10101"),
        GlowWeatherRegion("incheon", "인천", "서울·경기", 37.4563, 126.7052, 112, 109, "11B00000", "11B20201"),
        GlowWeatherRegion("suwon", "수원", "서울·경기", 37.2636, 127.0286, 119, 109, "11B00000", "11B20601"),
        GlowWeatherRegion("chuncheon", "춘천", "강원영서", 37.8813, 127.7298, 101, 105, "11D10000", "11D10301"),
        GlowWeatherRegion("gangneung", "강릉", "강원영동", 37.7519, 128.8761, 105, 105, "11D20000", "11D20501"),
        GlowWeatherRegion("daejeon", "대전", "대전·세종·충남", 36.3504, 127.3845, 133, 133, "11C20000", "11C20401"),
        GlowWeatherRegion("cheongju", "청주", "충청북도", 36.6424, 127.4890, 131, 131, "11C10000", "11C10301"),
        GlowWeatherRegion("jeonju", "전주", "전북자치도", 35.8242, 127.1480, 146, 146, "11F10000", "11F10201"),
        GlowWeatherRegion("gwangju", "광주", "광주·전남", 35.1595, 126.8526, 156, 156, "11F20000", "11F20501"),
        GlowWeatherRegion("daegu", "대구", "대구·경북", 35.8714, 128.6014, 143, 143, "11H10000", "11H10701"),
        GlowWeatherRegion("busan", "부산", "부산·울산·경남", 35.1796, 129.0756, 159, 159, "11H20000", "11H20201"),
        GlowWeatherRegion("ulsan", "울산", "부산·울산·경남", 35.5384, 129.3114, 152, 159, "11H20000", "11H20101"),
        GlowWeatherRegion("jeju", "제주", "제주도", 33.4996, 126.5312, 184, 184, "11G00000", "11G00201"),
        GlowWeatherRegion("seogwipo", "서귀포", "제주도", 33.2541, 126.5601, 189, 184, "11G00000", "11G00401"),
    )

    fun find(regionId: String?): GlowWeatherRegion =
        all.firstOrNull { it.id == regionId?.trim()?.lowercase() } ?: all.first()
}
