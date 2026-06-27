import net.ltgt.gradle.errorprone.errorprone

plugins {
    `java-library`
    id("com.gradleup.shadow") version "8.3.5"
    id("com.diffplug.spotless") version "6.25.0"
    id("net.ltgt.errorprone") version "4.1.0"
    id("com.github.spotbugs") version "6.0.26"
}

val keycloakVersion: String by project
val keycloakAdminClientVersion: String by project
val junitVersion: String by project
val mockitoVersion: String by project
val assertjVersion: String by project
val wiremockVersion: String by project
val testcontainersVersion: String by project
val keycloakTestcontainerVersion: String by project
val htmlunitVersion: String by project
val jacksonVersion: String by project
val slf4jVersion: String by project
val libphonenumberVersion: String by project

java {
    toolchain {
        // Keycloak 26.x runs on Java 21+. Build reproducibly against 21.
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
}

// ---------------------------------------------------------------------------
// Source sets: fast unit/integration tests (`test`) vs Docker-backed e2e
// (`e2eTest`). Keeping them separate means `./gradlew test` never needs Docker.
// ---------------------------------------------------------------------------
sourceSets {
    create("e2eTest") {
        compileClasspath += sourceSets["main"].output
        runtimeClasspath += sourceSets["main"].output
    }
}

val e2eTestImplementation: Configuration by configurations.getting {
    extendsFrom(configurations.testImplementation.get())
}
configurations["e2eTestRuntimeOnly"].extendsFrom(configurations.testRuntimeOnly.get())

// The dasniko Keycloak testcontainer pulls a server-versioned admin client that
// is not published; pin the admin client to its own latest 26.x release.
configurations.matching { it.name.startsWith("e2eTest") }.configureEach {
    resolutionStrategy.force("org.keycloak:keycloak-admin-client:$keycloakAdminClientVersion")
    // Newer Testcontainers is required to talk to recent Docker Engine API versions.
    resolutionStrategy.force(
        "org.testcontainers:testcontainers:$testcontainersVersion",
        "org.testcontainers:junit-jupiter:$testcontainersVersion")
}

dependencies {
    // --- Keycloak SPI: provided by the server at runtime, never bundled -----
    compileOnly("org.keycloak:keycloak-server-spi:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-server-spi-private:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-services:$keycloakVersion")
    compileOnly("org.keycloak:keycloak-core:$keycloakVersion")
    compileOnly("jakarta.ws.rs:jakarta.ws.rs-api:3.1.0")
    compileOnly("org.jboss.logging:jboss-logging:3.6.3.Final")
    // Jackson ships inside Keycloak; compile against it but do not bundle it.
    compileOnly("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")

    // --- Bundled runtime deps (shaded into the provider jar) ----------------
    // libphonenumber handles per-region trunk prefixes and validation correctly;
    // Keycloak does not ship it, so it must be bundled.
    implementation("com.googlecode.libphonenumber:libphonenumber:$libphonenumberVersion")

    // --- Unit / integration tests ------------------------------------------
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("org.mockito:mockito-core:$mockitoVersion")
    testImplementation("org.mockito:mockito-junit-jupiter:$mockitoVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("org.wiremock:wiremock:$wiremockVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    // Keycloak APIs are compileOnly for main; tests that touch SPI types need them.
    testImplementation("org.keycloak:keycloak-server-spi:$keycloakVersion")
    testImplementation("org.keycloak:keycloak-server-spi-private:$keycloakVersion")
    testImplementation("org.keycloak:keycloak-core:$keycloakVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testRuntimeOnly("org.slf4j:slf4j-simple:$slf4jVersion")

    // --- E2E (Testcontainers + browser) ------------------------------------
    e2eTestImplementation("org.testcontainers:testcontainers:$testcontainersVersion")
    e2eTestImplementation("org.testcontainers:junit-jupiter:$testcontainersVersion")
    e2eTestImplementation("com.github.dasniko:testcontainers-keycloak:$keycloakTestcontainerVersion")
    e2eTestImplementation("org.keycloak:keycloak-admin-client:$keycloakAdminClientVersion")
    e2eTestImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    // Drive the Keycloak login pages directly with HtmlUnit's WebClient (no browser).
    e2eTestImplementation("org.htmlunit:htmlunit:$htmlunitVersion")

    // --- Static analysis ----------------------------------------------------
    errorprone("com.google.errorprone:error_prone_core:2.36.0")
    spotbugsPlugins("com.h3xstream.findsecbugs:findsecbugs-plugin:1.13.0")
}

// ---------------------------------------------------------------------------
// Static analysis & formatting
// ---------------------------------------------------------------------------
spotless {
    java {
        target("src/**/*.java")
        palantirJavaFormat("2.50.0")
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

spotbugs {
    effort.set(com.github.spotbugs.snom.Effort.MAX)
    reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    excludeFilter.set(file("config/spotbugs/exclude.xml"))
}

// Analyse production code only; tests are not the security surface.
listOf("spotbugsTest", "spotbugsE2eTest").forEach { name ->
    tasks.matching { it.name == name }.configureEach { enabled = false }
}

tasks.spotbugsMain {
    reports.create("html") { required.set(true) }
    reports.create("xml") { required.set(false) }
}

tasks.withType<JavaCompile>().configureEach {
    options.errorprone {
        disableWarningsInGeneratedCode.set(true)
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

val e2eTest by tasks.registering(Test::class) {
    description = "Runs Docker-backed end-to-end tests (Keycloak + mocked BulkGate)."
    group = "verification"
    testClassesDirs = sourceSets["e2eTest"].output.classesDirs
    classpath = sourceSets["e2eTest"].runtimeClasspath
    useJUnitPlatform()
    shouldRunAfter(tasks.test)
    // The provider jar must exist before Keycloak loads it.
    dependsOn(tasks.shadowJar)
    systemProperty("provider.jar", tasks.shadowJar.get().archiveFile.get().asFile.absolutePath)
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.shadowJar {
    // Single deployable provider jar named <name>-<version>.jar (bundles
    // libphonenumber; all Keycloak/Jackson classes stay provided by the server).
    archiveClassifier.set("")
}

// Only ship the shaded jar — disable the thin jar so build/libs holds one artifact.
tasks.named<Jar>("jar") {
    enabled = false
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
