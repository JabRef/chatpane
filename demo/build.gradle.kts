plugins {
    application
    id("chatpane.java-conventions")
}

dependencies {
    implementation(project(":chatpane"))
    compileOnly(libs.jspecify)
    implementation(libs.slf4j.api)
    // The SLF4J backend (MADR 0005). Nothing `requires` these two: SLF4J finds
    // slf4j-tinylog, and tinylog-api finds tinylog-impl, through service
    // binding on the module path.
    runtimeOnly(libs.slf4j.tinylog)
    runtimeOnly(libs.tinylog.impl)
}

application {
    mainModule = "org.jabref.chatpane.demo"
    mainClass = "org.jabref.chatpane.demo.DemoApp"
    // JavaFX loads its native libraries (glass, prism) itself; Java 25 warns on
    // restricted native access and will block it later (JEP 472) unless granted.
    applicationDefaultJvmArgs = listOf("--enable-native-access=javafx.graphics",
            "-Dstdout.encoding=UTF-8", "-Dstderr.encoding=UTF-8")
}
