package com.sleekydz86.idolglow.sdom.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import java.sql.Types

@Entity
@Table(name = "tb_domain_list")
class SdomEntity(
    @Id
    @Column(name = "domain_id", length = 20)
    var domainId: String = "",
    @Column(name = "domain_nm", length = 100)
    var domainNm: String? = null,
    @Column(name = "domain_path", length = 200)
    var domainPath: String? = null,
    @Column(name = "domain_dc", length = 500)
    var domainDc: String? = null,
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_at", length = 1)
    var useAt: String? = null,
    @JdbcTypeCode(Types.CHAR)
    @Column(name = "actvty_at", length = 1)
    var actvtyAt: String? = null,
    @Column(name = "sort_seq")
    var sortSeq: Int? = null,
)
