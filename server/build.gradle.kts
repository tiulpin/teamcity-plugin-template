plugins {
    alias(libs.plugins.teamcity.server)
    alias(libs.plugins.teamcity.environments)
}

dependencies {
    compileOnly(libs.teamcity.server.api)
    compileOnly(libs.teamcity.server.web)

    implementation(project(":common"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)

    agent(project(path = ":agent", configuration = "plugin"))
}

teamcity {
    version = providers.gradleProperty("teamcityVersion").get()

    server {
        descriptor {
            name             = providers.gradleProperty("pluginName").get()
            displayName      = providers.gradleProperty("pluginDisplayName").get()
            version          = providers.gradleProperty("pluginVersion").get()
            vendorName       = providers.gradleProperty("pluginVendor").get()
            vendorUrl        = providers.gradleProperty("pluginVendorUrl").get()
            email            = providers.gradleProperty("pluginVendorEmail").get()
            description      = providers.gradleProperty("pluginDescription").get()
            minimumBuild     = providers.gradleProperty("pluginMinimumBuild").get()

            useSeparateClassloader    = true
            allowRuntimeReload        = true
            nodeResponsibilitiesAware = true
        }

        publish {
            token = providers.environmentVariable("MARKETPLACE_TOKEN").orNull
            channels = listOf("Stable")
        }

        archiveName = providers.gradleProperty("pluginName").get()
    }

    environments {
        downloadsDir = layout.buildDirectory.dir("downloads").get().asFile.path
        baseHomeDir  = layout.buildDirectory.dir("servers").get().asFile.path
        baseDataDir  = layout.buildDirectory.dir("data").get().asFile.path

        create("teamcity") {
            version = providers.gradleProperty("teamcityVersion").get()
            serverOptions("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5500")
            agentOptions("-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5501")
        }
    }
}

tasks.serverPlugin {
    dependsOn(tasks.test)
}
