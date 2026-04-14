plugins {
    kotlin("jvm") version "2.3.20"
    id("com.gradleup.shadow") version "9.4.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
}

version = "1.0.0"
group = "com.monta.otel"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    val otelVersion = "2.26.1"
    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelVersion-alpha"))
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv")
    implementation("io.opentelemetry.contrib:opentelemetry-samplers:1.54.0-alpha")
    // Dependency for HTTP response header customization
    implementation("io.opentelemetry.javaagent:opentelemetry-javaagent-extension-api")
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
    jvmToolchain(25)
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
