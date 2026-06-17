package com.sleekydz86.idolglow.image.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import com.sleekydz86.idolglow.image.domain.domainservice.ImageStorage
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

data class GeneratedFilename(
    val uniqueFilename: String,
    val extension: String
)
