plugins {
    kotlin("jvm")
    `maven-publish`
}

group = "com.ritesh.cashiro"
version = "0.1.0-SNAPSHOT"

// Use root project's Java toolchain; avoid forcing downloads here

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.7.1")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.1")
}
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = group.toString()
            artifactId = "parser-core"
            version = version.toString()
        }
    }
}

tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed", "standardOut", "standardError")

        // Show detailed information for each test
        showExceptions = true
        showCauses = true
        showStackTraces = true

        // Show standard output from println statements
        showStandardStreams = true

        // Display test results in a more readable format
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }

    maxParallelForks = maxOf(1, Runtime.getRuntime().availableProcessors() / 2)
}