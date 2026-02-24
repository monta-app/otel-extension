package com.monta.otel.extension;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseCustomizer;
import io.opentelemetry.javaagent.bootstrap.http.HttpServerResponseMutator;

/**
 * Automatically injects trace ID into HTTP response headers.
 * <p>
 * The trace ID is extracted from the current OpenTelemetry context and added as a
 * response header to aid in debugging and request tracing.
 * <p>
 * Configuration:
 * - OTEL_RESPONSE_HEADERS_ENABLED: Set to "true" to enable trace ID injection (default: disabled)
 */
public class ResponseHeaderCustomizer implements HttpServerResponseCustomizer {

    private static final String TRACE_ID_HEADER = "Trace-Id";
    private static final String ENABLED_ENV_VAR = "OTEL_RESPONSE_HEADERS_ENABLED";
    private static final boolean ENABLED = "true".equalsIgnoreCase(System.getenv(ENABLED_ENV_VAR));

    @Override
    public <RESPONSE> void customize(
            Context context,
            RESPONSE response,
            HttpServerResponseMutator<RESPONSE> responseMutator) {
        if (!ENABLED) {
            return;
        }

        SpanContext spanContext = Span.fromContext(context).getSpanContext();
        if (spanContext.isValid()) {
            String traceId = spanContext.getTraceId();
            responseMutator.appendHeader(response, TRACE_ID_HEADER, traceId);
        }
    }
}
