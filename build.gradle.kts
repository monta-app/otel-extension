val kotlinVersion: String by project

plugins {
    kotlin("jvm") version "1.9.10"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "11.6.0"
}

version = "1.0.0"
group = "com.monta.otel"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    val otelVersion = "1.30.0"
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure:$otelVersion")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv:1.21.0-alpha")
    implementation("io.opentelemetry.contrib:opentelemetry-samplers:$otelVersion-alpha")
}

sourceSets {
    main {
        java.srcDirs("src/main/kotlin")
        resources.srcDirs("src/main/resources")
    }
    test {
        java.srcDirs("src/test/kotlin")
        resources.srcDirs("src/test/resources")
    }
}

kotlin {
    jvmToolchain(17)
}

ktlint {
    filter {
        exclude("**/generated/**")
        include("**/kotlin/**")
    }
}

tasks {
    shadowJar {
        archiveBaseName.set("otel-extension")
        archiveVersion.set("")
        archiveClassifier.set("")
    }
    processResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    processTestResources {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
}
