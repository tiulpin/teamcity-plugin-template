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
        val runType = SampleRunType(registry)
        verify { registry.registerRunType(any<RunType>()) }
        assertEquals(SampleConstants.RUNNER_TYPE, runType.type)
    }

    @Test
    fun `default parameters carry the canonical message`() {
        val runType = SampleRunType(mockk(relaxed = true))
        assertEquals(
            SampleConstants.DEFAULT_MESSAGE,
            runType.defaultRunnerProperties?.get(SampleConstants.MESSAGE_PARAM),
        )
    }
}
