package com.sleekydz86.idolglow.architecture

import com.sleekydz86.idolglow.architecture.support.ArchitectureSourceScanner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

@DisplayName("Controller Direct Repository Baseline")
class ControllerDirectRepositoryBaselineTest {
    @DisplayName("controller가 repository를 직접 참조하는 건수가 기준값을 초과하지 않는다")
    @Test
    fun controllerDirectRepositoryCountStaysWithinBaseline() {

        // given
        val sourceRoot = 메인코틀린소스루트()

        // when
        val 위반목록 = ArchitectureSourceScanner.controllerRepository직접참조목록(sourceRoot)

        // then
        assertTrue(
            위반목록.size <= 기준_위반_파일_수,
            buildString {
                appendLine(
                    "controller → repository 직접 참조는 기준값을 초과할 수 없습니다 (기준=$기준_위반_파일_수, 실제=${위반목록.size})",
                )
                appendLine("docs/architecture/dependency-rule.md 참고")
                위반목록.forEach { appendLine(" - $it") }
            },
        )
    }

    companion object {
        private const val 기준_위반_파일_수 = 0

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
