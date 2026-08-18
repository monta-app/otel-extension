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

// Consumers run a mix of JDK 21 and JDK 25 runtimes. Compiling against the toolchain default
// produced class file version 69, which fails premain on every JDK 21 service and silently
// disables their instrumentation, so the release target is pinned to the lowest runtime in use.
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

tasks {
    test {
        useJUnitPlatform()
        // ExtensionArtifactTest inspects the published jar, so it has to exist first.
        dependsOn(shadowJar)
        systemProperty("otel.extension.jar", shadowJar.flatMap { it.archiveFile }.get().asFile.absolutePath)
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
