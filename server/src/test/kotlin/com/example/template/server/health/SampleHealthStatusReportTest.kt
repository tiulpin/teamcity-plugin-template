package com.example.template.server.health

import jetbrains.buildServer.serverSide.BuildTypeTemplate
import jetbrains.buildServer.serverSide.SBuildType
import jetbrains.buildServer.serverSide.SProject
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItemConsumer
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusScope
import jetbrains.buildServer.serverSide.healthStatus.ItemSeverity
import jetbrains.buildServer.vcs.SVcsRoot
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SampleHealthStatusReportTest {

    @Test
    fun `produces a single global health item with WARN severity`() {
        val report = SampleHealthStatusReport()
        val recorded = mutableListOf<HealthStatusItem>()

        val consumer = object : HealthStatusItemConsumer {
            override fun consumeGlobal(item: HealthStatusItem) { recorded += item }
            override fun consumeForProject(project: SProject, item: HealthStatusItem) = error("not expected")
            override fun consumeForBuildType(buildType: SBuildType, item: HealthStatusItem) = error("not expected")
            override fun consumeForTemplate(template: BuildTypeTemplate, item: HealthStatusItem) = error("not expected")
            override fun consumeForVcsRoot(vcsRoot: SVcsRoot, item: HealthStatusItem) = error("not expected")
        }

        val scope = mockk<HealthStatusScope> {
            every { isItemWithSeverityAccepted(any()) } returns true
        }

        assertTrue(report.canReportItemsFor(scope))
        report.report(scope, consumer)

        assertEquals(1, recorded.size)
        assertEquals(ItemSeverity.WARN, recorded[0].severity)
    }
}
