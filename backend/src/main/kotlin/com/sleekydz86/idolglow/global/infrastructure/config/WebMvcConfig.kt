package com.sleekydz86.idolglow.global.infrastructure.config

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUserArgumentResolver
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Files
import java.nio.file.Paths

@Configuration
class WebMvcConfig(
    private val loginUserArgumentResolver: LoginUserArgumentResolver,
    @Value("\${app.storage.local.base-path:}") private val localStorageBasePath: String,
) : WebMvcConfigurer {

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginUserArgumentResolver)
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val root = localStorageBasePath.trim().takeIf { it.isNotEmpty() }
            ?: Paths.get(System.getProperty("user.home"), "Desktop", "image").toString()
        registerUploadPath(registry, root, "profile-avatars", "/uploads/profile-avatars/**")
        registerUploadPath(registry, root, "webzine", "/uploads/webzine/**")
    }

    private fun registerUploadPath(
        registry: ResourceHandlerRegistry,
        root: String,
        childDirectory: String,
        resourcePattern: String,
    ) {
        val dir = Paths.get(root, childDirectory).toAbsolutePath().normalize()
        runCatching { Files.createDirectories(dir) }
        val location = dir.toUri().toString()
        val withSlash = if (location.endsWith("/")) location else "$location/"
        registry.addResourceHandler(resourcePattern)
            .addResourceLocations(withSlash)
    }
}
