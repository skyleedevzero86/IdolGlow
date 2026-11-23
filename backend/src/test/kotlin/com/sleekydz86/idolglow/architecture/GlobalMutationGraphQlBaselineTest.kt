package com.sleekydz86.idolglow.architecture

import com.sleekydz86.idolglow.architecture.support.ArchitectureSourceScanner
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.nio.file.Paths

@DisplayName("글로벌 GraphQL 뮤테이션 기준")
class GlobalMutationGraphQlBaselineTest {
    @DisplayName("global 모듈 graphql 에 도메인 mutation 핸들러가 없다")
    @Test
    fun globalGraphqlHasNoDomainMutations() {

        // given
        val sourceRoot = 메인코틀린소스루트()

        // when
        val mutationHandlers = ArchitectureSourceScanner.globalGraphQlMutation목록(sourceRoot)

        // then
        assertTrue(
            mutationHandlers.isEmpty(),
            buildString {
                appendLine("global.adapter.graphql 에 @MutationMapping 이 있으면 안 됩니다 (도메인 resolver로 분리)")
                mutationHandlers.forEach { appendLine(" - $it") }
            },
        )
    }

    companion object {
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
