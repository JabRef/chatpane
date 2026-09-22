pluginManagement {
    // Convention plugin shared by :chatpane and :demo (Java toolchain, JavaFX
    // platform variants, tests) — see build-logic/.
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "chatpane"

include(":chatpane")
include(":demo")
