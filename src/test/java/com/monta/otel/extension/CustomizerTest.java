package com.monta.otel.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    private static final String SENSITIVE_KEY = "otel.instrumentation.sanitization.url.experimental.sensitive-query-parameters";

    @Test
    void capturesTheForceTraceHeader() {
        Map<String, String> resolved = applyPropertiesCustomizer(Map.of());

        assertEquals("Force-Trace", resolved.get(CAPTURE_KEY));
    }

    /** A service configuring its own captured headers must keep them. */
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

    @Test
    void redactsMontaCredentialParametersOnTopOfTheAgentDefaults() {
        Map<String, String> resolved = applyPropertiesCustomizer(Map.of());

        String configured = resolved.get(SENSITIVE_KEY);
        assertTrue(configured.startsWith("AWSAccessKeyId,Signature,sig,X-Goog-Signature,"),
                "the agent defaults must survive, setting the property replaces them: " + configured);
        for (String param : Customizer.MONTA_SENSITIVE_QUERY_PARAMETERS) {
            assertTrue(List.of(configured.split(",")).contains(param), "missing " + param + " in " + configured);
        }
    }

    @Test
    void keepsSensitiveParametersTheServiceAlreadyConfigured() {
        Map<String, String> resolved = applyPropertiesCustomizer(Map.of(SENSITIVE_KEY, "card_number,token"));

        List<String> configured = List.of(resolved.get(SENSITIVE_KEY).split(","));
        assertEquals("card_number", configured.get(0), "the service's own list must come first: " + configured);
        assertEquals(configured.size(), configured.stream().distinct().count(), "duplicates in " + configured);
    }

    private static ConfigProperties stubConfig(Map<String, String> existing) {
        return (ConfigProperties)
                Proxy.newProxyInstance(
                        CustomizerTest.class.getClassLoader(),
                        new Class<?>[] {ConfigProperties.class},
                        (proxy, method, args) -> {
                            if ("getList".equals(method.getName())) {
                                String value = existing.get((String) args[0]);
                                if (value != null) {
                                    return List.of(value.split(","));
                                }
                                // getList(name, defaultValue) falls back to the caller's default
                                return args.length > 1 ? args[1] : List.of();
                            }
                            return null;
                        });
    }

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

        ConfigProperties config = stubConfig(existing);
        Map<String, String> resolved = new java.util.HashMap<>(existing);
        for (Function<ConfigProperties, Map<String, String>> customizer : customizers) {
            resolved.putAll(customizer.apply(config));
        }
        return resolved;
    }
}
