# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an OpenTelemetry SDK extension that provides custom sampling and resource configuration. The extension automatically configures OpenTelemetry instrumentation with environment-specific settings and intelligent sampling rules.

## Build Commands

- `./gradlew build` - Build the project and run all checks
- `./gradlew shadowJar` - Create a fat JAR for distribution (output: `build/libs/otel-extension.jar`)
- `./gradlew clean` - Clean build artifacts

## Usage

Example of how to run with the otel agent:
```
JAVA_TOOL_OPTIONS: -javaagent:/home/app/opentelemetry-javaagent.jar -Dotel.javaagent.extensions=/home/app/otel-extension.jar
```

## Architecture

### SPI Extension Pattern
The project uses Java's Service Provider Interface (SPI) for automatic discovery:
- `Customizer.java` implements `AutoConfigurationCustomizerProvider`
- Registered in `src/main/resources/META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider`
- OpenTelemetry SDK automatically discovers and loads this extension

### Custom Resource Configuration
The extension adds these resource attributes from environment variables:
- `service.instance.id` - Random UUID generated at startup
- `service.name` - From `SERVICE_NAME` environment variable
- `deployment.environment.name` - From `STAGE` environment variable  
- `service.version` - From `BUILD_NUMBER` environment variable

### Intelligent Sampling Strategy
- **Base Sampler**: Parent-based sampling with custom routing rules
- **Dropped Endpoints**: Health checks (`/health*`), metrics (`/prometheus*`, `/metrics*`)
- **Environment-based Rates**:
  - Production (`STAGE=production`): 10% sampling rate
  - Non-production: 100% sampling rate
  - Override with `OTEL_TRACES_SAMPLER_ARG` environment variable
- **Endpoint Dropping Configuration**:
  - Configure endpoints to be dropped via the `OTEL_INSTRUMENTATION_HTTP_SERVER_EXCLUDED_URL_PATHS` environment variable.
  - The value should be a comma-separated list of paths (e.g., `/health*,/metrics*`).
  - Defaults to `/health*,/prometheus*,/metrics*` if not set.
- **Forced Tracing via Request Header**:
  - Enable with `OTEL_FORCE_TRACE_HEADER_ENABLED=true` (default: disabled).
  - When enabled, any request containing the `Force-Trace: true` header is always sampled, bypassing all other sampling rules.
  - The header is automatically configured for capture as a span attribute.

### Key Dependencies
- OpenTelemetry SDK 2.17.0 with instrumentation BOM
- OpenTelemetry Contrib Samplers for rule-based routing
- Shadow plugin for creating distributable JAR

## File Structure

- `src/main/java/com/monta/otel/extension/Customizer.java` - Main extension implementation
- `src/main/java/com/monta/otel/extension/ForcedTracingSampler.java` - Forced tracing via request header
- `src/main/java/com/monta/otel/extension/ResponseHeaderCustomizer.java` - Trace ID response header injection
- `src/main/resources/META-INF/services/` - SPI service registration
- `build.gradle.kts` - Gradle build configuration with shadow JAR setup
- `gradle.properties` - JVM settings for builds (4GB heap)