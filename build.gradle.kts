import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.changelog)
}

val pluginGroupValue   = providers.gradleProperty("pluginGroup").get()
val pluginVersionValue = providers.gradleProperty("pluginVersion").get()
val javaVersionValue   = providers.gradleProperty("javaVersion").get().toInt()

allprojects {
    group   = pluginGroupValue
    version = pluginVersionValue

    repositories {
        mavenCentral()
        maven("https://download.jetbrains.com/teamcity-repository")
    }
}

subprojects {
    apply(plugin = rootProject.libs.plugins.kotlin.jvm.get().pluginId)

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersionValue))
        }
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            apiVersion.set(KotlinVersion.KOTLIN_2_0)
            languageVersion.set(KotlinVersion.KOTLIN_2_0)
        }
    }

    tasks.withType<Test>().configureEach {
        useJUnit()
    }
}

changelog {
    version.set(pluginVersionValue)
    path.set(file("CHANGELOG.md").canonicalPath)
    headerParserRegex.set("""\d+\.\d+\.\d+""".toRegex())
    keepUnreleasedSection.set(true)
    unreleasedTerm.set("[Unreleased]")
    groups.set(listOf("Added", "Changed", "Deprecated", "Removed", "Fixed", "Security"))
}
