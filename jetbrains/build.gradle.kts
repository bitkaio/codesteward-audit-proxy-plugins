plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.22"
    id("org.jetbrains.intellij.platform") version "2.2.1"
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
        intellijIdeaCommunity("2024.1")
        bundledPlugin("Git4Idea")
        instrumentationTools()
    }
}

kotlin {
    jvmToolchain(17)
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
            name = "Bitkai"
        }
        ideaVersion {
            sinceBuild = "241"
        }
    }
}
