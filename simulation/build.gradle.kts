plugins {
    id("application")
    id("com.github.spotbugs") version "6.0.7"
}

group = "uct.csc3003s.biofilm2"
version = "0.1-PROTOTYPE"

application {
    mainClass.set("uct.csc3003s.biofilm2.Simulation")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.opencsv:opencsv:5.9")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.0")

    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")
}

tasks.test {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}

// SpotBugs configuration
spotbugs {
    ignoreFailures.set(true) // Allow build to continue even with bugs found
    showStackTraces.set(true)
    showProgress.set(true)
    effort.set(com.github.spotbugs.snom.Effort.DEFAULT)
    reportLevel.set(com.github.spotbugs.snom.Confidence.MEDIUM) // Focus on medium+ priority issues
    reportsDir.set(layout.buildDirectory.dir("reports/spotbugs"))
    
    // Optional: exclude certain bug patterns
    // excludeFilter.set(file("spotbugs-exclude.xml"))
}

tasks.withType<com.github.spotbugs.snom.SpotBugsTask> {
    reports {
        create("html") {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("reports/spotbugs/spotbugs.html"))
            setStylesheet("fancy-hist.xsl")
        }
        create("xml") {
            required.set(true)
            outputLocation.set(layout.buildDirectory.file("reports/spotbugs/spotbugs.xml"))
        }
    }
}

// Convenient task to run all static analysis
tasks.register("staticAnalysis") {
    group = "verification"
    description = "Run all static analysis tools"
    dependsOn("spotbugsMain", "spotbugsTest")
}
