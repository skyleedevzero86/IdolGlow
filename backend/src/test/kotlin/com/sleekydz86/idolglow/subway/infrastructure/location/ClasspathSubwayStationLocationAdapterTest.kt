package com.sleekydz86.idolglow.subway.infrastructure.location

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.sleekydz86.idolglow.subway.infrastructure.order.ClasspathSubwayStationOrderAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("역 좌표 CSV 어댑터")
class ClasspathSubwayStationLocationAdapterTest {
    private val adapter = ClasspathSubwayStationLocationAdapter()

    @DisplayName("노선 파일의 모든 역이 좌표를 가진다")
    @Test
    fun everyStationOnOurLinesHasLocation() {
        val order = ClasspathSubwayStationOrderAdapter(jacksonObjectMapper())
        val lineIds = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "AREX", "SUINBUNDANG")

        val missing =
            lineIds
                .flatMap { order.orderedStops(it) }
                .map { it.name }
                .filter { adapter.locate(it) == null }

        assertTrue(missing.isEmpty(), "좌표 없는 역: $missing")
    }

    @DisplayName("괄호 부기와 역 접미사를 떼고, 이름이 바뀐 역은 새 이름으로 찾는다")
    @Test
    fun normalizesNamesAndFollowsRenamedStations() {
        val cityHall = adapter.locate("시청역")

        assertNotNull(cityHall)
        assertEquals(37.5646, cityHall!!.latitude, 0.005)
        assertEquals(126.9771, cityHall.longitude, 0.005)
        assertNotNull(adapter.locate("당고개"))
        assertNotNull(adapter.locate("동대문운동장"))
    }
}
