package com.sleekydz86.idolglow.eventinfo.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "event_meta_projection")
class EventMetaProjection(
    @Id
    @Column(name = "global_id", nullable = false, length = 36)
    val globalId: String,
    @Column(name = "meta_version", nullable = false)
    var metaVersion: Long,
    @Column(nullable = false, length = 20)
    var status: String,
    @Column(name = "merged_into", length = 36)
    var mergedInto: String?,
    @Column(nullable = false, length = 255)
    var title: String,
    @Column(name = "start_date")
    var startDate: LocalDate?,
    @Column(name = "end_date")
    var endDate: LocalDate?,
    @Column(length = 255)
    var venue: String?,
) : BaseEntity() {
    fun isNewerThanOrSame(version: Long): Boolean = metaVersion >= version

    fun apply(
        version: Long,
        snapshot: EventMetaSnapshot,
    ) {
        metaVersion = version
        status = snapshot.status
        mergedInto = snapshot.mergedInto
        title = snapshot.title
        startDate = snapshot.startDate
        endDate = snapshot.endDate
        venue = snapshot.venue
    }

    companion object {
        fun from(
            globalId: String,
            version: Long,
            snapshot: EventMetaSnapshot,
        ): EventMetaProjection =
            EventMetaProjection(
                globalId = globalId,
                metaVersion = version,
                status = snapshot.status,
                mergedInto = snapshot.mergedInto,
                title = snapshot.title,
                startDate = snapshot.startDate,
                endDate = snapshot.endDate,
                venue = snapshot.venue,
            )
    }
}
