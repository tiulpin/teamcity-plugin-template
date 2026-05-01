package com.example.template.agent.runner

import com.example.template.common.SampleConstants
import io.mockk.mockk
import jetbrains.buildServer.agent.BuildAgentConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleAgentBuildRunnerTest {

    private val runner = SampleAgentBuildRunner()

    @Test
    fun `runner type matches the shared constant`() {
        assertEquals(SampleConstants.RUNNER_TYPE, runner.type)
    }

    @Test
    fun `runner can run on any agent`() {
        assertTrue(runner.canRun(mockk<BuildAgentConfiguration>()))
    }

    @Test
    fun `runner info returns itself`() {
        assertTrue(runner.runnerInfo === runner)
    }
}
