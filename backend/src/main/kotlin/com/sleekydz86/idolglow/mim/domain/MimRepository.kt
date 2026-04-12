package com.sleekydz86.idolglow.mim.domain

interface MimRepository {
    fun findList(criteria: MimListCriteria): List<MimItem>
    fun count(criteria: MimListCriteria): Int
    fun findById(imageId: String): MimItem?
    fun insert(item: MimItem)
    fun update(item: MimItem)
    fun delete(imageId: String)
}
