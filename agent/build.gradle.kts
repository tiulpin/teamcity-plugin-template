plugins {
    alias(libs.plugins.teamcity.agent)
}

dependencies {
    compileOnly(libs.teamcity.agent.api)

    implementation(project(":common"))

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
}

teamcity {
    version = providers.gradleProperty("teamcityVersion").get()

    agent {
        descriptor {
            pluginDeployment {
                useSeparateClassloader = true
            }
        }

        archiveName = providers.gradleProperty("pluginName").get()
    }
}

tasks.agentPlugin {
    dependsOn(tasks.test)
}
