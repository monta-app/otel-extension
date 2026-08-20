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

// Resolved only so tests can read the agent jar; never on the runtime classpath.
val javaagent: Configuration by configurations.creating

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

    javaagent(libs.opentelemetry.javaagent)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

// Consumers run a mix of JDK 21 and JDK 25 runtimes. A jar compiled for 25 fails premain on 21:
// the agent aborts and the service runs with no instrumentation and no error.
tasks.withType<JavaCompile>().configureEach {
    options.release = 21
}

tasks {
    test {
        useJUnitPlatform()
        // ExtensionArtifactTest inspects the published jar, so it has to exist first.
        dependsOn(shadowJar)
        systemProperty("otel.extension.jar", shadowJar.flatMap { it.archiveFile }.get().asFile.absolutePath)
        systemProperty("otel.extension.release", compileJava.get().options.release.get().toString())
        systemProperty("otel.javaagent.jar", javaagent.singleFile.absolutePath)
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
