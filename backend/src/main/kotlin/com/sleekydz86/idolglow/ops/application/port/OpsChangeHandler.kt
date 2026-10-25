package com.sleekydz86.idolglow.ops.application.port

interface OpsChangeHandler {
    val type: String

    fun validate(
        target: String,
        payload: String,
    )

    fun snapshot(target: String): String

    fun apply(
        target: String,
        payload: String,
    )

    fun revert(
        target: String,
        beforeSnapshot: String,
    )
}
