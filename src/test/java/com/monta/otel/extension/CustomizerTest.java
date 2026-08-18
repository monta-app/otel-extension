package com.monta.otel.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizer;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

class CustomizerTest {

    /**
     * Pins the literal property key. The agent silently ignores keys it does not recognise, so a
     * rename here disables header capture with a green build and no runtime error.
     */
    @Test
    void suppliesTheHeaderCapturePropertyByItsExactKey() {
        Map<String, String> supplied = capturedProperties();

        assertEquals(
                "Force-Trace",
                supplied.get("otel.instrumentation.http.server.capture-request-headers"),
                "expected the Force-Trace header to be captured under the key the agent reads;"
                        + " supplied properties were: "
                        + supplied);
    }

    private static Map<String, String> capturedProperties() {
        Map<String, String> supplied = new HashMap<>();
        AutoConfigurationCustomizer recording =
                (AutoConfigurationCustomizer)
                        Proxy.newProxyInstance(
                                CustomizerTest.class.getClassLoader(),
                                new Class<?>[] {AutoConfigurationCustomizer.class},
                                (proxy, method, args) -> {
                                    if ("addPropertiesSupplier".equals(method.getName())) {
                                        @SuppressWarnings("unchecked")
                                        Supplier<Map<String, String>> supplier =
                                                (Supplier<Map<String, String>>) args[0];
                                        supplied.putAll(supplier.get());
                                    }
                                    return proxy;
                                });

        new Customizer().customize(recording);
        return supplied;
    }
}
