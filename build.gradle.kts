import com.install4j.gradle.Install4jTask
import de.undercouch.gradle.tasks.download.Download
import org.gradle.internal.os.OperatingSystem

plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.install4j)
    alias(libs.plugins.download)
    jacoco
    java
    distribution
    idea
    alias(libs.plugins.jmh)
    alias(libs.plugins.fx2j)
    alias(libs.plugins.test.logger)
    alias(libs.plugins.javafx)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

val fafUidVersion: String = libs.versions.faf.uid.get()
val javafxPlatformProperty = property("javafxPlatform").toString()
val useJfxFromProperties = javafxPlatformProperty != "unspecified"
val javafxPlatform: String = if (useJfxFromProperties) {
    javafxPlatformProperty
} else {
    val os = OperatingSystem.current()
    when {
        os.isLinux -> "linux"
        os.isMacOsX -> if (System.getProperty("os.arch") == "aarch64") "mac-aarch64" else "mac"
        os.isWindows -> "win"
        else -> javafxPlatformProperty
    }
}
println("Platform is: $javafxPlatform")

val nativeDir = layout.buildDirectory.dir("resources/native")

val mockitoAgent = configurations.create("mockitoAgent")

repositories {
    mavenCentral()
    maven { url = uri("https://maven.ej-technologies.com/repository") }
}

javafx {
    version = libs.versions.javafx.get()
    modules = listOf(
        "javafx.base", "javafx.controls", "javafx.graphics", "javafx.swing", "javafx.fxml", "javafx.web", "javafx.media"
    )
    if (useJfxFromProperties) {
        println("Using javaFxPlatform from properties $javafxPlatform")
        setPlatform(javafxPlatform)
    }
}

dependencies {
    implementation(libs.spring.boot.starter.core)
    implementation(libs.spring.boot.starter.cache)
    implementation(libs.spring.boot.starter.logging)
    implementation(libs.spring.boot.starter.websocket)
    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.webclient)
    implementation(libs.spring.boot.starter.oauth2.client)
    implementation(libs.spring.boot.jackson2)
    implementation(libs.spring.web)
    implementation(libs.spring.websocket)

    implementation(libs.reactor.extra)
    implementation(libs.reactor.tools)

    implementation(libs.bundles.faf.commons) {
        exclude(module = "guava")
    }
    implementation(libs.guava)
    implementation(libs.commons.compress)
    implementation(libs.bundles.jna)
    implementation(libs.jetbrains.annotations)
    implementation(libs.nv.i18n)
    implementation(libs.luaj.jse)
    implementation(libs.commons.validator) {
        exclude(module = "commons-logging")
    }
    implementation(libs.jackson.datatype.jsr310)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.q.builders)
    implementation(libs.jskills)
    implementation(libs.jsonapi.converter)
    implementation(libs.caffeine)
    implementation(libs.fxstage)
    implementation(libs.jfx.waitomo.theme)
    implementation(libs.irc.client)
    implementation(libs.mapstruct.core)
    implementation(libs.steamworks4j)

    implementation(libs.controlsfx)
    implementation(libs.javafxsvg) {
        exclude(module = "commons-logging")
    }
    implementation(libs.flowless)

    implementation(libs.javassist)
    implementation(libs.checker.qual)

    implementation(libs.fx2j.api)
    implementation(libs.install4j.runtime)

    testImplementation(libs.instancio.junit)
    testImplementation(libs.bundles.okhttp)
    testImplementation(libs.reactor.test)
    testImplementation(libs.bundles.testfx) {
        exclude(module = "guava")
    }
    testImplementation(libs.reactor.netty)
    testImplementation(libs.hamcrest.reflection)
    testImplementation(libs.spring.boot.starter.test)

    annotationProcessor(libs.spring.boot.configuration.processor)
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.mapstruct.processor)
    testAnnotationProcessor(libs.lombok)
    testAnnotationProcessor(libs.mapstruct.processor)
    compileOnly(libs.lombok)
    testCompileOnly(libs.lombok)

    mockitoAgent(libs.mockito.core) {
        isTransitive = false
    }
}

install4j {
    findProperty("install4jHomeDir")?.let { installDir = file(it) }
    findProperty("install4jLicense")?.let { license = it.toString() }
}

fx2j {
    modularizeIfPossible = false
    basePackage = "com.faforever.client.fx2j"
    strict = true
}

testlogger {
    setTheme("standard-parallel")
    showFullStackTraces = false
    showPassed = false
    showStandardStreams = false
}

tasks {
    bootJar {
        enabled = false
    }

    jar {
        enabled = true
        manifest {
            attributes(
                "Main-Class" to "com.faforever.client.Main",
                "Implementation-Title" to "FAF Client",
                "Implementation-Version" to archiveVersion,
                "Implementation-Vendor" to "Forged Alliance Forever",
            )
        }
    }

    processResources {
        val projectVersion = project.version.toString()
        inputs.property("projectVersion", projectVersion)
        filesMatching("**/application.yml") {
            filter { it.replace("#faf-client.version#", projectVersion) }
        }
    }

    jacocoTestReport {
        reports {
            xml.required = true
            html.required = false
        }
    }

    test {
        useJUnitPlatform()
        dependsOn("fx2jJar")
        systemProperty("junit.jupiter.execution.parallel.enabled", true)
        testLogging {
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
        maxHeapSize = "1024m"
        val agentPath = mockitoAgent.asPath
        jvmArgumentProviders.add(CommandLineArgumentProvider { listOf("-javaagent:$agentPath") })
    }

    bootRun {
        mainClass = "com.faforever.client.Main"
        jvmArgs(
            "-DnativeDir=build/resources/native",
            "-Dprism.forceGPU=true",
            "-Xmx1g",
            "-XX:MinHeapFreeRatio=15",
            "-XX:MaxHeapFreeRatio=33",
            "-XX:+HeapDumpOnOutOfMemoryError",
            "-XX:+UseZGC",
            "-Dio.netty.allocator.type=unpooled",
            "-Djava.net.preferIPv4Stack=true",
            "-XX:TieredStopAtLevel=1",
            // Needed for getting process handle via reflection
            "--add-opens=java.base/java.lang=ALL-UNNAMED",
        )
        if (OperatingSystem.current().isMacOsX) {
            // Fix HeadlessException in install4j's StartupNotification (FileOpeningHandler)
            jvmArgs("-Djava.awt.headless=false")
            // Activate 'mac' profile so @Profile("!mac") beans (e.g. DiscordRichPresenceService) are excluded
            environment("SPRING_PROFILES_ACTIVE", "prod,mac")
        }
    }
}

val downloadUnixUid = tasks.register<Download>("downloadUnixUid") {
    group = "native dependencies"
    src("https://github.com/FAForever/uid/releases/download/$fafUidVersion/faf-uid")
    dest(nativeDir.map { it.file("faf-uid") })
    onlyIfNewer(true)
    doLast {
        nativeDir.get().file("faf-uid").asFile.setExecutable(true, false)
    }
}

val downloadWindowsUid = tasks.register<Download>("downloadWindowsUid") {
    group = "native dependencies"
    src("https://github.com/FAForever/uid/releases/download/$fafUidVersion/faf-uid.exe")
    dest(nativeDir.map { it.file("faf-uid.exe") })
    onlyIfNewer(true)
}

val downloadMacOsUid = tasks.register<Download>("downloadMacOsUid") {
    group = "native dependencies"
    src("https://github.com/FAForever/uid/releases/download/$fafUidVersion/faf-uid-macos")
    dest(nativeDir.map { it.file("faf-uid-macos") })
    onlyIfNewer(true)
    doLast {
        val macosUid = nativeDir.get().file("faf-uid-macos").asFile
        // On macOS hosts, downloaded binaries get tagged with
        // `com.apple.quarantine` and Gatekeeper blocks execution until
        // the xattr is removed. Strip it on macOS builds; the command
        // exits non-zero if the attr isn't set, which we ignore.
        if (OperatingSystem.current().isMacOsX) {
            ProcessBuilder("xattr", "-d", "com.apple.quarantine", macosUid.absolutePath).start().waitFor()
        }
        macosUid.setExecutable(true, false)
    }
}

val downloadNativeDependencies = tasks.register("downloadNativeDependencies") {
    group = "native dependencies"
    dependsOn(downloadUnixUid, downloadWindowsUid, downloadMacOsUid)
}

tasks.compileJava {
    dependsOn(tasks.processResources, downloadNativeDependencies)
}

distributions {
    main {
        contents {
            from(
                configurations.runtimeClasspath,
                tasks.jar,
                downloadUnixUid,
                downloadWindowsUid,
                downloadMacOsUid,
            )
            exclude("**/i4jruntime*.jar")
        }
    }
}

tasks.register<Install4jTask>("buildInstall4jMediaFiles") {
    group = "distribution"
    description = "Creates install4j media files"
    dependsOn("installDist", "fx2jJar")

    projectFile = file("faf-client.install4j")
    release = project.version.toString()

    when (javafxPlatform) {
        "linux" -> mediaTypes = listOf("unixInstaller", "unixArchive")
        "mac" -> mediaTypes = listOf("macos", "macosFolder", "macosArchive", "macosFolderArchive")
        "win" -> mediaTypes = listOf("windows", "windowsArchive")
    }

    val hasInstall4jHomeDir = project.hasProperty("install4jHomeDir")
    val buildVersion = project.version.toString()
    doFirst {
        println("Building version $buildVersion")
        if (!hasInstall4jHomeDir) {
            throw GradleException("Specify install4jHomeDir in gradle.properties and set it to an install4j installation directory")
        }
    }
}
