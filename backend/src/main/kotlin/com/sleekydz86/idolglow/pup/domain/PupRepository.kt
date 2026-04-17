package com.sleekydz86.idolglow.pup.domain

interface PupRepository {
    fun findList(criteria: PupListCriteria): List<PupItem>
    fun count(criteria: PupListCriteria): Int
    fun findById(popupId: String): PupItem?
    fun findPublicByDomain(domainId: String): List<PupItem>
    fun insert(item: PupItem)
    fun update(item: PupItem)
    fun delete(popupId: String)
}
