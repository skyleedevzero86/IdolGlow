package com.sleekydz86.idolglow.subway.infrastructure.location

import com.sleekydz86.idolglow.subway.application.port.out.SubwayStationLocationPort
import com.sleekydz86.idolglow.subway.domain.SubwayStationLocation
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import java.nio.charset.Charset

@Component
class ClasspathSubwayStationLocationAdapter : SubwayStationLocationPort {
    private val locations: Map<String, SubwayStationLocation> by lazy(::readCsv)

    override fun locate(stationName: String): SubwayStationLocation? {
        val key = normalize(stationName)
        return locations[key] ?: RENAMED[key]?.let { locations[it] }
    }

    private fun readCsv(): Map<String, SubwayStationLocation> {
        val res = ClassPathResource(CSV_PATH)
        if (!res.exists()) {
            return emptyMap()
        }
        return res.inputStream
            .bufferedReader(Charset.forName("MS949"))
            .useLines { lines ->
                lines
                    .drop(1)
                    .mapNotNull(::parseRow)
                    .distinctBy { it.first }
                    .toMap()
            }
    }

    private fun parseRow(line: String): Pair<String, SubwayStationLocation>? {
        val cols = line.trim().removePrefix("\"").removeSuffix("\"").split("\",\"")
        val latitude = cols.getOrNull(LATITUDE)?.toDoubleOrNull()
        val longitude = cols.getOrNull(LONGITUDE)?.toDoubleOrNull()
        if (latitude == null || longitude == null) {
            return null
        }
        return normalize(cols[NAME]) to SubwayStationLocation(latitude, longitude)
    }

    companion object {
        private const val CSV_PATH = "data/서울시 역사마스터 정보.csv"
        private const val NAME = 1
        private const val LATITUDE = 3
        private const val LONGITUDE = 4
        private val PARENTHESES = Regex("\\(.*?\\)")
        private val RENAMED =
            mapOf(
                "당고개" to "불암산",
                "동대문운동장" to "동대문역사문화공원",
            )

        fun normalize(name: String): String = name.replace(PARENTHESES, "").trim().removeSuffix("역")
    }
}
