package com.sleekydz86.idolglow.productpackage.option.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.productpackage.option.application.OptionQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class OptionGraphQlController(
    private val optionQueryService: OptionQueryService,
) {
    @QueryMapping
    fun options(): List<OptionGraphQlResponse> =
        optionQueryService
            .findOptions()
            .map(OptionGraphQlResponse::from)

    @QueryMapping
    fun option(
        @Argument id: String,
    ): OptionGraphQlResponse =
        OptionGraphQlResponse.from(
            optionQueryService.findOption(id.toGraphQlIdLong("id")),
        )
}
