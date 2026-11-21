package com.sleekydz86.idolglow.global.llm

object DisabledLlmCallTracer : LlmCallTracer {
    override fun isEnabled(): Boolean = false

    override fun record(trace: LlmCallTrace) = Unit
}
