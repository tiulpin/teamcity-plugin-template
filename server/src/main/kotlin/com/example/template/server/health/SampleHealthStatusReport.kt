package com.example.template.server.health

import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItem
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusItemConsumer
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusReport
import jetbrains.buildServer.serverSide.healthStatus.HealthStatusScope
import jetbrains.buildServer.serverSide.healthStatus.ItemCategory
import jetbrains.buildServer.serverSide.healthStatus.ItemSeverity

// SAMPLE: delete or adapt. INFO items are hidden by the default Warning filter; using WARN here so the demo is visible.
class SampleHealthStatusReport : HealthStatusReport() {

    override fun getType(): String = REPORT_TYPE
    override fun getDisplayName(): String = "Sample plugin status"
    override fun getCategories(): Collection<ItemCategory> = listOf(CATEGORY)
    override fun canReportItemsFor(scope: HealthStatusScope): Boolean =
        scope.isItemWithSeverityAccepted(ItemSeverity.WARN)

    override fun report(scope: HealthStatusScope, resultConsumer: HealthStatusItemConsumer) {
        val item = HealthStatusItem(
            "$REPORT_TYPE-loaded",
            CATEGORY,
            mapOf("message" to "TeamCity plugin template is loaded.")
        )
        resultConsumer.consumeGlobal(item)
    }

    companion object {
        const val REPORT_TYPE = "sample-plugin-status"

        private val CATEGORY = ItemCategory(
            "sample-plugin-category",
            "Sample plugin",
            ItemSeverity.WARN
        )
    }
}
