import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.language.jvm.tasks.ProcessResources
import org.springframework.boot.gradle.tasks.bundling.BootWar
import org.springframework.boot.gradle.tasks.run.BootRun

plugins {
    java
    war
    id("org.springframework.boot") version "3.4.5"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.graalvm.buildtools.native") version "0.10.6"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

val frontendDir = layout.projectDirectory.dir("frontend")
val frontendBuildDir = frontendDir.dir("dist")

fun npmCommand(vararg args: String): List<String> {
    val isWindows = System.getProperty("os.name").lowercase().contains("win")
    return if (isWindows) listOf("cmd", "/c", "npm", *args) else listOf("npm", *args)
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    providedRuntime("org.springframework.boot:spring-boot-starter-tomcat")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events(TestLogEvent.FAILED, TestLogEvent.SKIPPED)
        exceptionFormat = TestExceptionFormat.FULL
    }
}

val npmInstall by tasks.registering(Exec::class) {
    workingDir = frontendDir.asFile
    commandLine(npmCommand("install"))
    inputs.files(
        frontendDir.file("package.json"),
        frontendDir.file("package-lock.json")
    )
    outputs.dir(frontendDir.dir("node_modules"))
}

val npmBuild by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    workingDir = frontendDir.asFile
    commandLine(npmCommand("run", "build"))
    inputs.dir(frontendDir.dir("src"))
    inputs.dir(frontendDir.dir("public"))
    inputs.files(frontendDir.file("package.json"), frontendDir.file("vite.config.js"), frontendDir.file("index.html"))
    outputs.dir(frontendBuildDir)
}

val npmTest by tasks.registering(Exec::class) {
    dependsOn(npmInstall)
    workingDir = frontendDir.asFile
    commandLine(npmCommand("run", "test", "--", "run"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(npmBuild)
    from(frontendBuildDir) {
        into("static")
    }
}

tasks.named("check") {
    dependsOn(npmTest)
}

tasks.named<BootRun>("bootRun") {
    dependsOn(npmBuild)
}

tasks.named<BootWar>("bootWar") {
    archiveFileName.set("plex-importer.war")
}

graalvmNative {
    binaries {
        named("main") {
            imageName.set("plex-importer")
        }
    }
}
