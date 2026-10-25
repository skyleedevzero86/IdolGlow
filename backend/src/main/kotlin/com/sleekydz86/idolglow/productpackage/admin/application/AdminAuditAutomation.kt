package com.sleekydz86.idolglow.productpackage.admin.application

data class AdminAuditAutomationChoice(
    val actionCode: String?,
    val reason: String,
)

object AdminAuditAutomation {
    private val keptByPerson = setOf(
        "OPS_CHANGE_APPROVE",
        "OPS_CHANGE_APPLY",
        "OPS_CHANGE_REVERT",
        "OPS_CHANGE_REJECT",
    )

    fun basis(localProfile: Boolean): String =
        if (localProfile) "로컬 데이터 기준" else "운영 데이터 기준"

    fun choose(actions: List<String>): AdminAuditAutomationChoice {
        if (actions.isEmpty()) {
            return AdminAuditAutomationChoice(null, "반복된 관리자 작업이 없어 자동화 대상을 고르지 않았다.")
        }
        return choiceFor(actions)
    }

    private fun choiceFor(actions: List<String>): AdminAuditAutomationChoice {
        val candidate = actions.firstOrNull { !keptForPerson(it) }
        if (candidate == null) {
            return AdminAuditAutomationChoice(
                actions.first(),
                "가장 많이 반복됐지만 승인, 적용, 되돌리기, 재결제라 사람이 처리한다.",
            )
        }
        return AdminAuditAutomationChoice(
            candidate,
            "가장 많이 반복되고 조회와 요약으로 줄일 수 있어 자동화 후보로 둔다.",
        )
    }

    private fun keptForPerson(actionCode: String): Boolean {
        if (actionCode in keptByPerson) {
            return true
        }
        return paymentAction(actionCode)
    }

    private fun paymentAction(actionCode: String): Boolean =
        actionCode.contains("CHARGE") || actionCode.contains("REFUND") || actionCode.contains("BILLING")
}
