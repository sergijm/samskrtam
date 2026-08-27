plugins {
    alias(libs.plugins.spring.boot)
    id("java")
    id("edu.sc.seis.launch4j") version "2.5.4"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(platform("org.springframework.boot:spring-boot-dependencies:${libs.versions.spring.boot.get()}"))

    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework:spring-jdbc")
    implementation(libs.postgresql.jdbc)
    implementation(libs.picocli)
    implementation(libs.jackson.dataformat.yaml)
    implementation(libs.jackson.databind)
    implementation(project(":shared:samskrtam-dtos"))

    implementation(libs.lombok)
    annotationProcessor(libs.lombok)

    testImplementation(libs.spring.test)
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Bundle the root samcli.yml as a classpath resource so the tool always has a
// default config even when launched from a different working directory.
tasks.processResources {
    from(rootProject.projectDir) {
        include("samcli.yml")
    }
    // Bundle the lemma-index SQL as a classpath fallback for `refresh-lemmas`.
    // Single source of truth remains etcetera/sql/lingua_index_lemmas.sql.
    from(rootProject.file("etcetera/sql")) {
        include("lingua_index_lemmas.sql")
        into("sql")
    }
}

// launch4j: wrap the Spring Boot fat jar into a native Windows .exe that simply
// launches it with the system-installed Java (thin wrapper, no bundled JRE).
launch4j {
    outfile = "samcli.exe"
    mainClassName = "sm.selflearn.samskrtam.samcli.SamcliApplication"
    jar = "${layout.buildDirectory.get()}/libs/samcli-${project.version}.jar"
    headerType = "console"
    jvmOptions = setOf("-Xmx256m")
    // Let launch4j find a Java on PATH / Windows registry; no bundled JRE.
    jreMinVersion = "21"
    dontWrapJar = false
}

tasks.named("launch4j") {
    dependsOn(tasks.named("bootJar"))
}

// After the wrapper is built, copy samcli.exe into the project root for convenience.
tasks.named("createExe") {
    doLast {
        val exe = layout.buildDirectory.file("launch4j/samcli.exe").get().asFile
        val dest = rootProject.layout.projectDirectory.file("samcli.exe").asFile
        exe.copyTo(dest, overwrite = true)
        logger.lifecycle("Copied launcher to {}", dest)
    }
}
