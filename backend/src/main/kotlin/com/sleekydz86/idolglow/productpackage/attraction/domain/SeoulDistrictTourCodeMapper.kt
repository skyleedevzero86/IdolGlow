package com.sleekydz86.idolglow.productpackage.attraction.domain

object SeoulDistrictTourCodeMapper {
    const val SEOUL_AREA_CODE: Int = 11

    private val signguCodeByDistrict: Map<String, Int> = mapOf(
        "강남구" to 11680,
        "강동구" to 11740,
        "강북구" to 11305,
        "강서구" to 11500,
        "관악구" to 11620,
        "광진구" to 11215,
        "구로구" to 11530,
        "금천구" to 11545,
        "노원구" to 11350,
        "도봉구" to 11320,
        "동대문구" to 11230,
        "동작구" to 11590,
        "마포구" to 11440,
        "서대문구" to 11410,
        "서초구" to 11650,
        "성동구" to 11200,
        "성북구" to 11290,
        "송파구" to 11710,
        "양천구" to 11470,
        "영등포구" to 11560,
        "용산구" to 11170,
        "은평구" to 11380,
        "종로구" to 11110,
        "중구" to 11140,
        "중랑구" to 11260,
    )

    fun signguCodeOf(districtLabel: String): Int? =
        signguCodeByDistrict[districtLabel.trim()]

    fun districtOf(signguCode: Int): String? =
        signguCodeByDistrict.entries.firstOrNull { it.value == signguCode }?.key

    fun resolveDistrictLabel(rawAddress: String): String? {
        val normalized = rawAddress.replace(" ", "")
        return signguCodeByDistrict.keys.firstOrNull { normalized.contains(it) }
    }
}
