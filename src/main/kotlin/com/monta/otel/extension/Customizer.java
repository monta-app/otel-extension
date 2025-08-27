package com.monta.otel.extension;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSampler;
import io.opentelemetry.contrib.sampler.RuleBasedRoutingSamplerBuilder;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.sdk.trace.SdkTracerProviderBuilder;
import io.opentelemetry.sdk.trace.samplers.Sampler;
import io.opentelemetry.semconv.ServiceAttributes;
import io.opentelemetry.semconv.UrlAttributes;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Note this class is wired into SPI via {@code
 * resources/META-INF/services/io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider}
 */
public class Customizer implements AutoConfigurationCustomizerProvider {

    private static final String OTEL_TRACES_EXCLUDED_URL_PATHS_ENV_VAR = "OTEL_TRACES_EXCLUDED_URL_PATHS";
    private static final String DEFAULT_EXCLUDED_URL_PATHS = "/health*,/prometheus*,/metrics*";

    @Override
    public void customize(AutoConfigurationCustomizer autoConfiguration) {

        // Add additional resource attributes programmatically
        autoConfiguration.addResourceCustomizer((resource, configProperties) ->
                resource.merge(
                        Resource.builder()
                                .put("service.instance.id", UUID.randomUUID().toString())
                                .put(ServiceAttributes.SERVICE_NAME, System.getenv("SERVICE_NAME"))
                                .put("deployment.environment.name", System.getenv("STAGE"))
                                .put(ServiceAttributes.SERVICE_VERSION, System.getenv("BUILD_NUMBER"))
                                .build()
                )
        );

        autoConfiguration.addTracerProviderCustomizer((builder, config) ->
                configureSampler(builder)
        );
    }

    /**
     * Configures the SdkTracerProviderBuilder with a rule-based routing sampler.
     */
    private SdkTracerProviderBuilder configureSampler(SdkTracerProviderBuilder builder) {
        RuleBasedRoutingSamplerBuilder samplerBuilder = RuleBasedRoutingSampler.builder(SpanKind.SERVER, getSampler());

        String excludedPaths = Optional.ofNullable(System.getenv(OTEL_TRACES_EXCLUDED_URL_PATHS_ENV_VAR))
                .orElse(DEFAULT_EXCLUDED_URL_PATHS);

        Arrays.stream(excludedPaths.split(","))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .forEach(path -> samplerBuilder.drop(UrlAttributes.URL_PATH, path));

        return builder.setSampler(Sampler.parentBased(samplerBuilder.build()));
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
            double rate = Objects.equals(System.getenv("STAGE"), "production") ? 0.1 : 1.0;
            return Sampler.traceIdRatioBased(rate);
        }
    }
}
