package com.monta.otel.extension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

/**
 * Checks that the configuration keys this extension sets are keys the agent actually reads.
 *
 * <p>The agent ignores unrecognised properties without warning, so a key that no longer exists
 * disables the behaviour it was meant to configure while every unit test stays green. Scanning the
 * agent jar for the literal is enough to catch that, and it does not require booting the agent.
 */
class AgentPropertyContractTest {

    private static final String CAPTURE_REQUEST_HEADERS = "otel.instrumentation.http.server.capture-request-headers";

    @Test
    void theAgentRecognisesTheCaptureRequestHeadersKey() throws IOException {
        assertTrue(
                agentJarContains(CAPTURE_REQUEST_HEADERS),
                CAPTURE_REQUEST_HEADERS
                        + " does not appear in the agent, so it configures nothing. Check the name against"
                        + " the agent version this extension is built for.");
    }

    private static boolean agentJarContains(String literal) throws IOException {
        Path agent = locateAgentJar();
        byte[] needle = literal.getBytes(StandardCharsets.UTF_8);

        try (JarFile jarFile = new JarFile(agent.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                if (indexOf(readAll(jarFile, entry), needle) >= 0) {
                    return true;
                }
            }
        }
        return false;
    }

    private static byte[] readAll(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream in = jarFile.getInputStream(entry)) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            in.transferTo(out);
            return out.toByteArray();
        }
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        outer:
        for (int i = 0; i <= haystack.length - needle.length; i++) {
            for (int j = 0; j < needle.length; j++) {
                if (haystack[i + j] != needle[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }

    private static Path locateAgentJar() {
        String configured = System.getProperty("otel.javaagent.jar");
        if (configured == null) {
            fail("otel.javaagent.jar system property is not set; see the test task in build.gradle.kts");
        }
        Path agent = Path.of(configured);
        if (!Files.isRegularFile(agent)) {
            fail("Java agent jar not found at " + agent);
        }
        return agent;
    }
}
