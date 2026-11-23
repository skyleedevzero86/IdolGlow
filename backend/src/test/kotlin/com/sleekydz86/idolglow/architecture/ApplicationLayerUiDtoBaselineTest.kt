package com.sleekydz86.idolglow.architecture

import com.sleekydz86.idolglow.architecture.support.ArchitectureSourceScanner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

@DisplayName("애플리케이션 계층 UI DTO 기준")
class ApplicationLayerUiDtoBaselineTest {
    @DisplayName("application 계층의 ui dto 참조 건수가 기준값을 초과하지 않는다")
    @Test
    fun applicationLayerUiDtoCountStaysWithinBaseline() {

        // given
        val sourceRoot = 메인코틀린소스루트()

        // when
        val 위반목록 = ArchitectureSourceScanner.application계층UiDto위반목록(sourceRoot)

        // then
        assertTrue(
            위반목록.size <= BASELINE_UI_DTO_VIOLATION_FILE_COUNT,
            buildString {
                appendLine(
                    "application 계층의 UI DTO 참조는 기준값을 초과할 수 없습니다 (기준=$BASELINE_UI_DTO_VIOLATION_FILE_COUNT, 실제=${위반목록.size})",
                )
                appendLine("docs/architecture/dependency-rule.md 참고")
                위반목록.forEach { appendLine(" - $it") }
            },
        )
    }

    companion object {
        private const val BASELINE_UI_DTO_VIOLATION_FILE_COUNT = 0

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
