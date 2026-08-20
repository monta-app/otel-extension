package com.monta.otel.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

class CustomizerTest {

    private static final String CAPTURE_KEY = "otel.instrumentation.http.server.capture-request-headers";

    @Test
    void capturesTheForceTraceHeader() {
        Map<String, String> resolved = applyPropertiesCustomizer(Map.of());

        assertEquals("Force-Trace", resolved.get(CAPTURE_KEY));
    }

    /**
     * A service configuring its own captured headers must keep them; dropping them would trade one
     * silent gap for another.
     */
    @Test
    void keepsHeadersTheServiceAlreadyCaptures() {
        Map<String, String> resolved = applyPropertiesCustomizer(Map.of(CAPTURE_KEY, "X-Request-Source"));

        assertEquals("X-Request-Source,Force-Trace", resolved.get(CAPTURE_KEY));
    }

    @Test
    void doesNotDuplicateTheHeaderWhenAlreadyPresent() {
        Map<String, String> resolved = applyPropertiesCustomizer(Map.of(CAPTURE_KEY, "force-trace"));

        assertEquals("force-trace", resolved.get(CAPTURE_KEY));
    }

    /** Minimal {@link ConfigProperties} that only answers the key under test. */
    private static ConfigProperties stubConfig(String capturedHeaders) {
        List<String> configured =
                capturedHeaders.isEmpty() ? List.of() : List.of(capturedHeaders.split(","));
        return (ConfigProperties)
                Proxy.newProxyInstance(
                        CustomizerTest.class.getClassLoader(),
                        new Class<?>[] {ConfigProperties.class},
                        (proxy, method, args) -> {
                            if ("getList".equals(method.getName())) {
                                return CAPTURE_KEY.equals(args[0]) ? configured : List.of();
                            }
                            return null;
                        });
    }

    /** Runs {@link Customizer} against a recording customizer and applies what it registered. */
    private static Map<String, String> applyPropertiesCustomizer(Map<String, String> existing) {
        List<Function<ConfigProperties, Map<String, String>>> customizers = new ArrayList<>();

        AutoConfigurationCustomizer recording =
                (AutoConfigurationCustomizer)
                        Proxy.newProxyInstance(
                                CustomizerTest.class.getClassLoader(),
                                new Class<?>[] {AutoConfigurationCustomizer.class},
                                (proxy, method, args) -> {
                                    if ("addPropertiesCustomizer".equals(method.getName())) {
                                        @SuppressWarnings("unchecked")
                                        Function<ConfigProperties, Map<String, String>> customizer =
                                                (Function<ConfigProperties, Map<String, String>>) args[0];
                                        customizers.add(customizer);
                                    }
                                    return proxy;
                                });

        new Customizer().customize(recording);

        ConfigProperties config = stubConfig(existing.getOrDefault(CAPTURE_KEY, ""));
        Map<String, String> resolved = new java.util.HashMap<>(existing);
        for (Function<ConfigProperties, Map<String, String>> customizer : customizers) {
            resolved.putAll(customizer.apply(config));
        }
        return resolved;
    }
}
