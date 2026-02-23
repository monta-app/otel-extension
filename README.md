# OpenTelemetry Extension

This is an OpenTelemetry SDK extension that provides custom sampling and resource configuration. The extension automatically configures OpenTelemetry instrumentation with environment-specific settings and intelligent sampling rules.

## Configuration

The extension can be configured using the following environment variables:

| Environment Variable             | Description                                                                                                                               | Default |
|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------|---------|
| `SERVICE_NAME`                   | The name of the service.                                                                                                                  | - |
| `STAGE`                          | The deployment stage (e.g., `production`, `staging`).                                                                                     | - |
| `BUILD_NUMBER`                   | The build number of the service.                                                                                                          | - |
| `OTEL_TRACES_SAMPLER_ARG`        | The sampling rate to use. Overrides the default sampling rate.                                                                            | `0.1` (prod), `1.0` (non-prod) |
| `OTEL_TRACES_EXCLUDED_URL_PATHS` | A comma-separated list of URL paths to exclude from tracing (e.g., `/health*,/metrics*`).                                                | `/health*,/prometheus*,/metrics*` |
| `OTEL_RESPONSE_HEADERS_ENABLED`  | Set to `true` to enable trace ID injection in response headers.                                                                           | `false` |

## Features

### Response Header Injection

The extension can inject OpenTelemetry trace IDs into HTTP response headers for debugging and correlation.

**Header Added:**
- `Trace-Id`: The trace ID (32-character hex string)

**Configuration:**
Set `OTEL_RESPONSE_HEADERS_ENABLED=true` to enable trace ID injection.

**Example Response Headers:**
```
Trace-Id: 4bf92f3577b34da6a3ce929d0e0e4736
```

**Use Cases:**
- End-to-end request tracing from client logs
- Correlation between frontend and backend traces
- Simplified debugging of distributed systems
- Customer support troubleshooting
