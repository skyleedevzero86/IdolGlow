package com.sleekydz86.idolglow.mim.infrastructure

import com.sleekydz86.idolglow.sdom.infrastructure.SdomEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import java.time.LocalDateTime
import java.sql.Types

@Entity
@Table(name = "tb_main_image")
class MimEntity(
    @Id
    @Column(name = "image_id", length = 20)
    var imageId: String = "",
    @Column(name = "domain_id", length = 20)
    var domainId: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", insertable = false, updatable = false)
    var domain: SdomEntity? = null,
    @Column(name = "image_nm", length = 200)
    var imageNm: String? = null,
    @Column(name = "image", length = 500)
    var image: String? = null,
    @Column(name = "image_file", length = 500)
    var imageFile: String? = null,
    @Column(name = "image_dc", length = 1000)
    var imageDc: String? = null,
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "reflct_at", length = 1)
    var reflctAt: String? = null,
    @Column(name = "frst_register_id", length = 50)
    var frstRegisterId: String? = null,
    @Column(name = "frst_regist_pnttm")
    var frstRegistPnttm: LocalDateTime? = null,
    @Column(name = "last_updusr_id", length = 50)
    var lastUpdusrId: String? = null,
    @Column(name = "last_updt_pnttm")
    var lastUpdtPnttm: LocalDateTime? = null,
)
