package com.sleekydz86.idolglow.ops.application.port

import com.sleekydz86.idolglow.ops.domain.OpsChangeRequest

interface OpsChangeRequestPort {
    fun save(request: OpsChangeRequest): OpsChangeRequest

    fun findByIdForUpdate(id: Long): OpsChangeRequest?

    fun findRecent(): List<OpsChangeRequest>
}
