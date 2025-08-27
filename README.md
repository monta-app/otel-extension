# OpenTelemetry Extension

This is an OpenTelemetry SDK extension that provides custom sampling and resource configuration. The extension automatically configures OpenTelemetry instrumentation with environment-specific settings and intelligent sampling rules.

## Configuration

The extension can be configured using the following environment variables:

| Environment Variable             | Description                                                                                                                               |
|----------------------------------| ----------------------------------------------------------------------------------------------------------------------------------------- |
| `SERVICE_NAME`                   | The name of the service.                                                                                                                  |
| `STAGE`                          | The deployment stage (e.g., `production`, `staging`).                                                                                     |
| `BUILD_NUMBER`                   | The build number of the service.                                                                                                          |
| `OTEL_TRACES_SAMPLER_ARG`        | The sampling rate to use. Overrides the default sampling rate.                                                                            |
| `OTEL_TRACES_EXCLUDED_URL_PATHS` | A comma-separated list of URL paths to exclude from tracing (e.g., `/health*,/metrics*`). Defaults to `/health*,/prometheus*,/metrics*`. |
