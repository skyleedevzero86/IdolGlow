package com.sleekydz86.idolglow.architecture.support

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText

object ArchitectureSourceScanner {

    private val applicationLayerPattern = Regex(
        """import\s+com\.sleekydz86\.idolglow\.[\w.]+\.(ui\.|adapter\.web\.(dto|request)\.)"""
    )

    private val packagePattern = Regex("""^package\s+([\w.`]+)""", RegexOption.MULTILINE)

    fun application계층UiDto위반목록(sourceRoot: Path): List<String> {
        if (!Files.exists(sourceRoot)) {
            return emptyList()
        }

        return sourceRoot.toFile()
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            .filter { file ->
                val relative = sourceRoot.relativize(file.toPath()).toString().replace('\\', '/')
                relative.contains("/application/")
            }
            .mapNotNull { file ->
                val path = file.toPath()
                val content = path.readText()
                if (applicationLayerPattern.containsMatchIn(content)) {
                    sourceRoot.relativize(path).toString().replace('\\', '/')
                } else {
                    null
                }
            }
            .sorted()
            .toList()
    }

    fun 패키지경로불일치목록(sourceRoot: Path): List<String> {
        if (!Files.exists(sourceRoot)) {
            return emptyList()
        }

        return sourceRoot.toFile()
            .walkTopDown()
            .filter { it.isFile && it.name.endsWith(".kt") }
            .mapNotNull { file ->
                val path = file.toPath()
                val content = path.readText()
                val packageName = packagePattern.find(content)?.groupValues?.get(1)
                    ?.replace("`", "")
                    ?: return@mapNotNull null

                val relativePath = sourceRoot.relativize(path).parent
                    ?.toString()
                    ?.replace('\\', '/')
                    ?: return@mapNotNull null

                val expectedPath = packageName.replace('.', '/')
                if (relativePath != expectedPath) {
                    "$relativePath (${path.name}) → package $packageName"
                } else {
                    null
                }
            }
            .sorted()
            .toList()
    }
}
