plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.1.10"
    id("org.jetbrains.intellij.platform") version "2.13.1"
}

group = "com.codesteward"
version = "0.1.0"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.3")
        bundledPlugin("Git4Idea")
        bundledPlugin("org.jetbrains.plugins.terminal")
    }
}

kotlin {
    jvmToolchain(21)
}

intellijPlatform {
    pluginConfiguration {
        id = "com.codesteward.audit-proxy"
        name = "Codesteward Audit Proxy"
        version = project.version.toString()
        description = """
            Configure the Codesteward audit proxy, inject identity metadata, and provide
            observability into what headers are being forwarded to AI coding agents.
        """.trimIndent()
        vendor {
            name = "bikaio"
        }
        ideaVersion {
            sinceBuild = "243"
        }
    }
}
