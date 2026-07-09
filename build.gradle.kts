plugins {
    java
    alias(libs.plugins.shadow)
}

version = providers.gradleProperty("version").getOrElse("1.0.0")
group = "com.monta.otel"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    implementation(platform(libs.opentelemetry.bom))
    implementation(libs.opentelemetry.sdk.autoconfigure)
    implementation(libs.opentelemetry.semconv)
    implementation(libs.opentelemetry.contrib.samplers)
    // Dependency for HTTP response header customization
    implementation(libs.opentelemetry.javaagent.extension.api)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.opentelemetry.sdk.testing)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
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
