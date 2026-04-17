package com.sleekydz86.idolglow.mbrd.domain

import java.util.UUID

interface MbrdEditorAssetRepository {
    fun save(asset: MbrdEditorAsset): MbrdEditorAsset
    fun findById(id: UUID): MbrdEditorAsset?
}
