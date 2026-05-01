package com.example.template.server.runner

import com.example.template.common.SampleConstants
import jetbrains.buildServer.serverSide.InvalidProperty
import jetbrains.buildServer.serverSide.PropertiesProcessor

class SampleRunnerPropertiesProcessor : PropertiesProcessor {
    override fun process(properties: Map<String, String>?): Collection<InvalidProperty> {
        val errors = mutableListOf<InvalidProperty>()
        val props = properties.orEmpty()

        if (props[SampleConstants.MESSAGE_PARAM].isNullOrBlank()) {
            errors += InvalidProperty(SampleConstants.MESSAGE_PARAM, "Message must not be blank.")
        }

        when (val repeat = props[SampleConstants.REPEAT_PARAM]?.toIntOrNull()) {
            null -> errors += InvalidProperty(SampleConstants.REPEAT_PARAM, "Repeat must be an integer.")
            in 1..SampleConstants.MAX_REPEAT -> Unit
            else -> errors += InvalidProperty(
                SampleConstants.REPEAT_PARAM,
                "Repeat must be between 1 and ${SampleConstants.MAX_REPEAT}.",
            )
        }
        return errors
    }
}
