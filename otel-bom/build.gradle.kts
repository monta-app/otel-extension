plugins {
    `java-library`
    `maven-publish`
}

version = rootProject.version
group = "com.monta.otel"

repositories {
    mavenCentral()
}

dependencies {
    api(platform(libs.opentelemetry.bom))
    api(libs.opentelemetry.api)
    api(libs.opentelemetry.sdk)
    api(libs.opentelemetry.sdk.autoconfigure)
    api(libs.opentelemetry.exporter.otlp)
    api(libs.opentelemetry.kafka.clients)
    runtimeOnly(libs.opentelemetry.logback.mdc)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "otel-bom"
            from(components["java"])
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/monta-app/otel-extension")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
