plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

version = "1.0.0"
group = "com.monta.otel"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    val otelVersion = "2.10.0"
    implementation(platform("io.opentelemetry.instrumentation:opentelemetry-instrumentation-bom-alpha:$otelVersion-alpha"))
    implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")
    implementation("io.opentelemetry.semconv:opentelemetry-semconv")
    implementation("io.opentelemetry.contrib:opentelemetry-samplers:1.41.0-alpha")
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
