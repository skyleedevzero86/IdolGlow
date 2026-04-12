package com.sleekydz86.idolglow.bnr.domain

interface BnrRepository {
    fun findList(criteria: BnrListCriteria): List<BnrItem>
    fun count(criteria: BnrListCriteria): Int
    fun findById(bannerId: String): BnrItem?
    fun insert(item: BnrItem)
    fun update(item: BnrItem)
    fun delete(bannerId: String)
}
