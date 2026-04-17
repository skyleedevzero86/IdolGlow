package com.sleekydz86.idolglow.sitecontent.application.port.`in`

import com.sleekydz86.idolglow.sitecontent.application.dto.SiteHomeContentResponse

interface SiteContentQueryUseCase {
    fun readHomeContent(): SiteHomeContentResponse
}
