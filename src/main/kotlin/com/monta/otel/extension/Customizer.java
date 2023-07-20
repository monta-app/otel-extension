package com.monta.otel.extension;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.resource.attributes.ResourceAttributes;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;

import java.util.UUID;

/**
 * Note this class is wired into SPI via {@code
 * resources/META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider}
 */
public class Customizer implements AutoConfigurationCustomizerProvider {

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {

        // Add additional resource attributes programmatically
        autoConfiguration.addResourceCustomizer((resource, configProperties) ->
                resource.merge(
                        Resource.builder()
                                .put(ResourceAttributes.SERVICE_INSTANCE_ID, UUID.randomUUID().toString())
                                .put(ResourceAttributes.SERVICE_NAME, System.getenv("SERVICE_NAME"))
                                .put(ResourceAttributes.DEPLOYMENT_ENVIRONMENT, System.getenv("STAGE"))
                                .put(ResourceAttributes.SERVICE_VERSION, System.getenv("BUILD_NUMBER"))
                                .build()
                )
        );

        // Set the sampler to be the default parentbased_always_on, but drop calls to spring
        // boot actuator endpoints
        autoConfiguration.addTracerProviderCustomizer((sdkTracerProviderBuilder, configProperties) ->
                sdkTracerProviderBuilder.setSampler(
                        Sampler.parentBased(
                                RuleBasedRoutingSampler.builder(SpanKind.SERVER, getSampler())
                                        .drop(SemanticAttributes.HTTP_TARGET, "/health*")
                                        .drop(SemanticAttributes.HTTP_TARGET, "/prometheus*")
                                        .build()
                        )
                )
        );
    }

    private static Sampler getSampler() {

        String otelTracesSamplerArg = System.getenv("OTEL_TRACES_SAMPLER_ARG");

        if (otelTracesSamplerArg != null) {
            try {
                double ratio = Double.parseDouble(otelTracesSamplerArg);
                return Sampler.traceIdRatioBased(ratio);
            } catch (Exception exception) {
                return Sampler.alwaysOff();
            }
        } else {
            return Sampler.alwaysOn();
        }
    }
}
