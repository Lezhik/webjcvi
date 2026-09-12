plugins {
    java
    id("org.springframework.boot") version "3.5.15"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "org.webjcvi"
version = "0.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
}

val jteVersion = "3.2.4"
val springAiVersion = "1.1.2"
val archUnitVersion = "1.4.1"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-webflux")
    implementation("gg.jte:jte-spring-boot-starter-3:$jteVersion")
    implementation("org.springframework.ai:spring-ai-starter-mcp-server-webflux:$springAiVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.projectreactor:reactor-test")
    testImplementation("com.tngtech.archunit:archunit-junit5:$archUnitVersion")
}

tasks.test {
    useJUnitPlatform()
    doFirst {
        val journalDir = rootProject.layout.projectDirectory.dir("reports/logs").asFile
        journalDir.mkdirs()
        val journal = journalDir.resolve("test.log")
        if (journal.exists() && !journal.delete()) {
            throw GradleException("Unable to clear previous test journal: ${journal.absolutePath}")
        }
    }
}

tasks.register<JavaExec>("analyzeLogs") {
    group = "webjcvi"
    description = "Run tests (DEBUG journal) then analyze reports/logs/test.log into reports/logs/report.json"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.webjcvi.logs.GenerateLogAnalysisMain")
    workingDir = rootProject.projectDir
    dependsOn(tasks.test)
}

tasks.register<JavaExec>("generateDnaReport") {
    group = "webjcvi"
    description = "Regenerate the DNA report from jcvi-dna.txt into build/reports/"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("org.webjcvi.report.GenerateReportMain")
    workingDir = rootProject.projectDir
}
