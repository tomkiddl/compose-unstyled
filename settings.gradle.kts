rootProject.name = "ComposeUnstyled"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}

dependencyResolutionManagement {
    repositories {
        google()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/kotlin/p/wasm/experimental")
    }
}

include(":core")
include(":demo-button")
include(":demo-checkbox")
include(":demo-dialog")
include(":demo-disclosure")
include(":demo-icon")
include(":demo-ios")
include(":demo-menu")
include(":demo-modalsheet")
include(":demo-progressindicator")
include(":demo-radiogroup")
include(":demo-scrollarea")
include(":demo-separators")
include(":demo-sheet")
include(":demo-slider")
include(":demo-tabgroup")
include(":demo-text")
include(":demo-textfield")
include(":demo-toggleswitch")