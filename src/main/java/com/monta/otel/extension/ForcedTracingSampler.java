package com.monta.otel.extension;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;

import java.util.List;

/**
 * A sampler that forces recording and sampling when the {@code Force-Trace} request header
 * is present with a value of {@code "true"} or {@code "1"}.
 * <p>
 * When triggered, the span is always sampled regardless of the delegate sampler's decision.
 * Otherwise, sampling falls through to the delegate.
 * <p>
 * Configuration:
 * - OTEL_FORCE_TRACE_HEADER_ENABLED: Set to "true" to enable forced tracing (default: disabled)
 */
public class ForcedTracingSampler implements Sampler {

    static final String FORCE_TRACE_HEADER = "Force-Trace";
    private static final String ENABLED_ENV_VAR = "OTEL_FORCE_TRACE_HEADER_ENABLED";

    static final AttributeKey<List<String>> HEADER_ATTRIBUTE_KEY =
            AttributeKey.stringArrayKey("http.request.header." + FORCE_TRACE_HEADER.toLowerCase());

    private final Sampler delegate;
    private final boolean enabled;

    public ForcedTracingSampler(Sampler delegate) {
        this(delegate, Boolean.parseBoolean(System.getenv(ENABLED_ENV_VAR)));
    }

    // Package-private constructor for testing
    ForcedTracingSampler(Sampler delegate, boolean enabled) {
        this.delegate = delegate;
        this.enabled = enabled;
    }

    @Override
    public SamplingResult shouldSample(
            Context parentContext,
            String traceId,
            String name,
            SpanKind spanKind,
            Attributes attributes,
            List<LinkData> parentLinks) {

        if (enabled) {
            List<String> headerValues = attributes.get(HEADER_ATTRIBUTE_KEY);
            if (headerValues != null && headerValues.stream().anyMatch(Boolean::parseBoolean)) {
                return SamplingResult.recordAndSample();
            }
        }

        return delegate.shouldSample(parentContext, traceId, name, spanKind, attributes, parentLinks);
    }

    @Override
    public String getDescription() {
        return "ForcedTracingSampler{" + delegate.getDescription() + "}";
    }
}
