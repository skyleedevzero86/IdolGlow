package com.sleekydz86.idolglow.pup.infrastructure

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
@Table(name = "tb_popup_manage")
class PupEntity(
    @Id
    @Column(name = "popup_id", length = 20)
    var popupId: String = "",
    @Column(name = "domain_id", length = 20)
    var domainId: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id", insertable = false, updatable = false)
    var domain: SdomEntity? = null,
    @Column(name = "popup_sj_nm", length = 200)
    var popupSjNm: String? = null,
    @Column(name = "file_url", length = 500)
    var fileUrl: String? = null,
    @Column(name = "link_target", length = 20)
    var linkTarget: String? = null,
    @Column(name = "popup_img_path", length = 500)
    var popupImgPath: String? = null,
    @Column(name = "popup_file_nm", length = 200)
    var popupFileNm: String? = null,
    @Column(name = "popup_vrticl_lc")
    var popupVrticlLc: Int? = null,
    @Column(name = "popup_width_lc")
    var popupWidthLc: Int? = null,
    @Column(name = "popup_vrticl_size")
    var popupVrticlSize: Int? = null,
    @Column(name = "popup_width_size")
    var popupWidthSize: Int? = null,
    @Column(name = "ntce_bgnde", length = 20)
    var ntceBgnde: String? = null,
    @Column(name = "ntce_endde", length = 20)
    var ntceEndde: String? = null,
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "stopvew_setup_at", length = 1)
    var stopvewSetupAt: String? = null,
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "ntce_at", length = 1)
    var ntceAt: String? = null,
    @Column(name = "frst_register_id", length = 50)
    var frstRegisterId: String? = null,
    @Column(name = "frst_regist_pnttm")
    var frstRegistPnttm: LocalDateTime? = null,
    @Column(name = "last_updusr_id", length = 50)
    var lastUpdusrId: String? = null,
    @Column(name = "last_updt_pnttm")
    var lastUpdtPnttm: LocalDateTime? = null,
)
