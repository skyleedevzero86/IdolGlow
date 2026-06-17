package com.sleekydz86.idolglow.architecture

import com.sleekydz86.idolglow.architecture.support.ArchitectureSourceScanner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

class PackagePathAlignmentBaselineTest {

    @Test
    fun `패키지_선언과_폴더_경로_불일치_건수가_기준값을_초과하지_않는다`() {
        // given
        val sourceRoot = 메인코틀린소스루트()

        // when
        val 불일치목록 = ArchitectureSourceScanner.패키지경로불일치목록(sourceRoot)

        // then
        assertTrue(
            불일치목록.size <= 기준_불일치_건수,
            buildString {
                appendLine(
                    "package/path 불일치 건수는 기준값을 초과할 수 없습니다 (기준=$기준_불일치_건수, 실제=${불일치목록.size})",
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
        private val 기준_불일치_건수: Int by lazy {
            ArchitectureSourceScanner.패키지경로불일치목록(메인코틀린소스루트()).size
        }

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
