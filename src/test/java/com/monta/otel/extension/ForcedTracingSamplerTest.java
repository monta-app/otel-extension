package com.monta.otel.extension;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.sdk.trace.samplers.SamplingDecision;
import io.opentelemetry.sdk.trace.samplers.SamplingResult;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ForcedTracingSamplerTest {

    private static final String TRACE_ID = "00000000000000000000000000000001";

    private SamplingResult sample(ForcedTracingSampler sampler, Attributes attributes) {
        return sampler.shouldSample(
                Context.root(),
                TRACE_ID,
                "test-span",
                SpanKind.SERVER,
                attributes,
                Collections.emptyList()
        );
    }

    @Test
    void forcesRecordAndSampleWhenHeaderIsTrueAndEnabled() {
        ForcedTracingSampler sampler = new ForcedTracingSampler(Sampler.alwaysOff(), true);

        Attributes attributes = Attributes.of(
                ForcedTracingSampler.HEADER_ATTRIBUTE_KEY, List.of("true")
        );

        assertEquals(SamplingDecision.RECORD_AND_SAMPLE, sample(sampler, attributes).getDecision());
    }

    @Test
    void forcesRecordAndSampleWhenHeaderIsTrueCaseInsensitiveAndEnabled() {
        ForcedTracingSampler sampler = new ForcedTracingSampler(Sampler.alwaysOff(), true);

        Attributes attributes = Attributes.of(
                ForcedTracingSampler.HEADER_ATTRIBUTE_KEY, List.of("TRUE")
        );

        assertEquals(SamplingDecision.RECORD_AND_SAMPLE, sample(sampler, attributes).getDecision());
    }

    @Test
    void delegatesToWrappedSamplerWhenHeaderIsAbsent() {
        ForcedTracingSampler sampler = new ForcedTracingSampler(Sampler.alwaysOff(), true);

        assertEquals(SamplingDecision.DROP, sample(sampler, Attributes.empty()).getDecision());
    }

    @Test
    void delegatesToWrappedSamplerWhenHeaderValueIsNotRecognised() {
        ForcedTracingSampler sampler = new ForcedTracingSampler(Sampler.alwaysOff(), true);

        Attributes attributes = Attributes.of(
                ForcedTracingSampler.HEADER_ATTRIBUTE_KEY, List.of("yes")
        );

        assertEquals(SamplingDecision.DROP, sample(sampler, attributes).getDecision());
    }

    @Test
    void delegatesToWrappedSamplerWhenDisabledEvenIfHeaderIsPresent() {
        ForcedTracingSampler sampler = new ForcedTracingSampler(Sampler.alwaysOff(), false);

        Attributes attributes = Attributes.of(
                ForcedTracingSampler.HEADER_ATTRIBUTE_KEY, List.of("true")
        );

        assertEquals(SamplingDecision.DROP, sample(sampler, attributes).getDecision());
    }

    @Test
    void descriptionIncludesDelegateDescription() {
        ForcedTracingSampler sampler = new ForcedTracingSampler(Sampler.alwaysOn(), true);

        assertEquals("ForcedTracingSampler{AlwaysOnSampler}", sampler.getDescription());
    }
}
