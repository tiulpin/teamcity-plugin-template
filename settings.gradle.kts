plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = providers.gradleProperty("pluginName").get()

include(":server", ":agent", ":common")
