package com.sleekydz86.idolglow.bnr.infrastructure

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
@Table(name = "tb_banner")
class BnrEntity(
    @Id
    @Column(name = "banner_id", length = 20)
    var bannerId: String = "",
    @Column(name = "domain_id", length = 20)
    var domainId: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", insertable = false, updatable = false)
    var domain: SdomEntity? = null,
    @Column(name = "banner_nm", length = 200)
    var bannerNm: String? = null,
    @Column(name = "link_url", length = 500)
    var linkUrl: String? = null,
    @Column(name = "banner_image", length = 500)
    var bannerImage: String? = null,
    @Column(name = "banner_image_file", length = 500)
    var bannerImageFile: String? = null,
    @Column(name = "banner_dc", length = 1000)
    var bannerDc: String? = null,
    @Column(name = "sort_ordr")
    var sortOrdr: Int? = null,
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
