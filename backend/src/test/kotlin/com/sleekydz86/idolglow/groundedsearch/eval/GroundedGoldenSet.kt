package com.sleekydz86.idolglow.groundedsearch.eval

object GroundedGoldenSet {
    fun cases(): List<GroundedEvalCase> = lines("grounded-search/golden.jsonl").map(GroundedGoldenParser::case)

    fun probes(): List<GroundedEvalProbe> = lines("grounded-search/validation.jsonl").map(GroundedGoldenParser::probe)

    private fun lines(path: String): List<String> {
        val stream =
            GroundedGoldenSet::class.java.classLoader.getResourceAsStream(path)
                ?: throw IllegalArgumentException("평가 파일을 찾을 수 없습니다. path=$path")
        return stream.bufferedReader().readLines().map { it.trim() }.filter { it.isNotEmpty() }
    }
}
