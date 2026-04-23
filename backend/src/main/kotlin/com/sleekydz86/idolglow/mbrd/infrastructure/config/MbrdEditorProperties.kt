package com.sleekydz86.idolglow.mbrd.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.mbrd")
data class MbrdEditorProperties(
    var imagePublicBasePath: String = "/api/mbrd/editor/images",
)
