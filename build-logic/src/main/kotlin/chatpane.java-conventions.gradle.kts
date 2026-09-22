// Shared by :chatpane and :demo: Java toolchain, JavaFX platform jars, tests.

import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.nativeplatform.MachineArchitecture
import org.gradle.nativeplatform.OperatingSystemFamily

plugins {
    java
    id("org.gradlex.jvm-dependency-conflict-resolution")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

// The plain org.openjfx Maven jars are empty; the code sits in classified jars
// (`-win`, `-linux`, …) that Gradle cannot pick on its own. One variant per
// platform is patched onto each module (as in JabRef,
// MADR 0002), so the module path gets real `javafx.*` modules for the host.
// The incubator modules (RichTextArea, MADR 0010) ship the same way.
listOf("javafx-base", "javafx-controls", "javafx-graphics",
        "jfx-incubator-input", "jfx-incubator-richtext").forEach { jfxModule ->
    // Matches the empty jars, so a missing platform fails loudly instead of compiling against nothing.
    addJfxTarget(jfxModule, "", "none", "none")
    addJfxTarget(jfxModule, "linux", OperatingSystemFamily.LINUX, MachineArchitecture.X86_64)
    addJfxTarget(jfxModule, "linux-aarch64", OperatingSystemFamily.LINUX, MachineArchitecture.ARM64)
    addJfxTarget(jfxModule, "mac", OperatingSystemFamily.MACOS, MachineArchitecture.X86_64)
    addJfxTarget(jfxModule, "mac-aarch64", OperatingSystemFamily.MACOS, MachineArchitecture.ARM64)
    addJfxTarget(jfxModule, "win", OperatingSystemFamily.WINDOWS, MachineArchitecture.X86_64)
}

fun addJfxTarget(jfxModule: String, name: String, os: String, arch: String) {
    jvmDependencyConflicts.patch.module("org.openjfx:$jfxModule") {
        addTargetPlatformVariant(name, os, arch)
    }
}

val hostOs: String = System.getProperty("os.name").lowercase().let {
    when {
        it.contains("windows") -> OperatingSystemFamily.WINDOWS
        it.contains("mac") -> OperatingSystemFamily.MACOS
        else -> OperatingSystemFamily.LINUX
    }
}
val hostArch: String = when (System.getProperty("os.arch")) {
    "aarch64" -> MachineArchitecture.ARM64
    else -> MachineArchitecture.X86_64
}
configurations.configureEach {
    // Resolvable-only: a consumable configuration carrying these attributes (OpenFastTrace's
    // oftRequirementConfig is both) becomes a second match for :demo's dependency on :chatpane.
    if (isCanBeResolved && !isCanBeConsumed) {
        attributes {
            attribute(OperatingSystemFamily.OPERATING_SYSTEM_ATTRIBUTE, objects.named(hostOs))
            attribute(MachineArchitecture.ARCHITECTURE_ATTRIBUTE, objects.named(hostArch))
        }
    }
}

// The same catalog the module build files use; a precompiled script plugin gets no typesafe
// `libs` accessor, hence the lookup by name.
val catalog = extensions.getByType<VersionCatalogsExtension>().named("libs")
fun lib(alias: String) = catalog.findLibrary(alias).orElseThrow { GradleException("No $alias in libs.versions.toml") }

dependencies {
    testImplementation(platform(lib("junit-bom")))
    testImplementation(lib("junit-jupiter"))
    testImplementation(lib("assertj-core"))
    testCompileOnly(lib("jspecify"))
    testRuntimeOnly(lib("junit-platform-launcher"))
    // Tests log through the same backend the demo uses (MADR 0005).
    testRuntimeOnly(lib("slf4j-tinylog"))
    testRuntimeOnly(lib("tinylog-impl"))
    // UI tests (MADR 0008): fx-labs fork of TestFX, EUPL-1.2 (weak copyleft), test scope only.
    testImplementation(lib("testfx-junit"))
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    // Warnings are errors while the code base is small enough to keep it at zero.
    // Not this-escape: a JavaFX control sets its style class and listeners in the
    // constructor by design, and every control would need a suppression.
    options.compilerArgs.addAll(listOf("-Xlint:all,-this-escape", "-Werror"))
}

// A failing test must be diagnosable from the console alone: CI uploads no
// test report, and an assertion's message is where its diagnosis lives.
fun Test.logFailuresInFull() {
    testLogging {
        events("failed")
        exceptionFormat = TestExceptionFormat.FULL
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

tasks.test {
    // UI tests (@Tag("ui")) need a display — kept out of `build` so it stays
    // green headless; they run via `uiTest`.
    useJUnitPlatform {
        excludeTags("ui")
    }
    logFailuresInFull()
}

tasks.register<Test>("uiTest") {
    group = "verification"
    description = "Runs the TestFX UI tests (needs a display or Xvfb)"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform {
        includeTags("ui")
    }
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // Keeps java.awt.Desktop unsupported, so no test can start a real browser or
    // file manager that outlives the JVM and holds Gradle's output pipe open.
    systemProperty("java.awt.headless", "true")
    logFailuresInFull()
}
