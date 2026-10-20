package com.sleekydz86.idolglow.architecture

import com.sleekydz86.idolglow.architecture.support.ArchitectureSourceScanner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

@DisplayName("Package Path Alignment Baseline")
class PackagePathAlignmentBaselineTest {
    @DisplayName("패키지 선언과 폴더 경로 불일치 건수가 기준값을 초과하지 않는다")
    @Test
    fun packagePathMismatchStaysWithinBaseline() {

        // given
        val sourceRoot = 메인코틀린소스루트()

        // when
        val 불일치목록 = ArchitectureSourceScanner.패키지경로불일치목록(sourceRoot)

        // then
        assertTrue(
            불일치목록.size <= BASELINE_MISMATCH_COUNT,
            buildString {
                appendLine(
                    "package/path 불일치 건수는 기준값을 초과할 수 없습니다 (기준=$BASELINE_MISMATCH_COUNT, 실제=${불일치목록.size})",
                )
                appendLine("docs/architecture/package-rule.md 참고")
                불일치목록.take(30).forEach { appendLine(" - $it") }
                if (불일치목록.size > 30) {
                    appendLine(" ... 외 ${불일치목록.size - 30}건")
                }
            },
        )
    }

    companion object {
        private const val BASELINE_MISMATCH_COUNT = 0

        private fun 메인코틀린소스루트(): Path {
            val cwd = Paths.get("").toAbsolutePath()
            val fromModule = cwd.resolve("src/main/kotlin")
            if (fromModule.toFile().exists()) {
                return fromModule
            }
            return cwd.resolve("backend/src/main/kotlin")
        }
    }
}
