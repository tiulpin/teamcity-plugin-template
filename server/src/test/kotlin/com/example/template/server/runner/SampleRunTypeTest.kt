package com.example.template.server.runner

import com.example.template.common.SampleConstants
import io.mockk.mockk
import io.mockk.verify
import jetbrains.buildServer.serverSide.RunType
import jetbrains.buildServer.serverSide.RunTypeRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class SampleRunTypeTest {

    @Test
    fun `registers itself with the registry on construction`() {
        val registry = mockk<RunTypeRegistry>(relaxed = true)
        val runType = SampleRunType(registry, mockk(relaxed = true))
        verify { registry.registerRunType(any<RunType>()) }
        assertEquals(SampleConstants.RUNNER_TYPE, runType.type)
    }

    @Test
    fun `default parameters carry the canonical message and repeat`() {
        val runType = SampleRunType(mockk(relaxed = true), mockk(relaxed = true))
        val defaults = runType.defaultRunnerProperties.orEmpty()
        assertEquals(SampleConstants.DEFAULT_MESSAGE, defaults[SampleConstants.MESSAGE_PARAM])
        assertEquals(SampleConstants.DEFAULT_REPEAT, defaults[SampleConstants.REPEAT_PARAM])
    }
}
