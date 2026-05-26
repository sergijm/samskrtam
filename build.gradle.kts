
// build.gradle.kts (корень монорепо)
plugins {
    alias(libs.plugins.kotlin.jvm)    apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot)   apply false
}

subprojects {
    group   = "sm.selflearn"
    version = "0.0.1-SNAPSHOT"
}
