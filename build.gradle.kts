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
        mavenLocal()
        mavenCentral()
    }
}

// Источники во всех модулях — UTF-8 (санскрит и кириллица в строках/комментариях).
// Без явной кодировки javac на Windows читает исходники в платформенной кодировке,
// что ломает не-ASCII-строки вида "aḥ"/"am" (мojibake).
subprojects {
    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
    }
}
