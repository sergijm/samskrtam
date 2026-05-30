plugins {
    // Применяем плагины к подпроектам, а не к корневому
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.spring.boot) apply false
}

allprojects {
    group = "sm.selflearn.samskrtam"
    version = "0.0.1-SNAPSHOT"

    repositories {
        mavenCentral()
    }
}
