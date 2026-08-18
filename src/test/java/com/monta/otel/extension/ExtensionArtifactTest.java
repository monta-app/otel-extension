package com.monta.otel.extension;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.junit.jupiter.api.Test;

/**
 * Guards the shape of the published jar rather than the behaviour of the code inside it.
 *
 * <p>Services consume this extension as a release asset on a mix of JDK 21 and JDK 25 runtimes. A
 * jar compiled for a newer runtime than the consumer throws {@code UnsupportedClassVersionError} in
 * premain; the agent then aborts and the JVM keeps serving with no instrumentation at all, which is
 * invisible until someone goes looking for traces that were never produced.
 */
class ExtensionArtifactTest {

    /** Java 21 is class file major version 65. */
    private static final int MAX_SUPPORTED_CLASS_FILE_MAJOR = 65;

    private static final String OWN_CLASS_PREFIX = "com/monta/";

    @Test
    void publishedJarRunsOnTheOldestSupportedRuntime() throws IOException {
        Path jar = locateJar();

        List<String> tooNew = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.startsWith(OWN_CLASS_PREFIX) || !name.endsWith(".class")) {
                    continue;
                }
                int major = readClassFileMajorVersion(jarFile, entry);
                if (major > MAX_SUPPORTED_CLASS_FILE_MAJOR) {
                    tooNew.add(name + " (class file major " + major + ")");
                }
            }
        }

        assertTrue(
                tooNew.isEmpty(),
                "These classes target a newer runtime than the oldest consumer (max major "
                        + MAX_SUPPORTED_CLASS_FILE_MAJOR
                        + "): "
                        + tooNew);
    }

    @Test
    void jarContainsTheExtensionItself() throws IOException {
        try (JarFile jarFile = new JarFile(locateJar().toFile())) {
            assertFalse(
                    jarFile.stream().noneMatch(e -> e.getName().equals("com/monta/otel/extension/Customizer.class")),
                    "shadowJar did not include Customizer, so the version check above proves nothing");
        }
    }

    private static int readClassFileMajorVersion(JarFile jarFile, JarEntry entry) throws IOException {
        try (InputStream in = jarFile.getInputStream(entry);
                DataInputStream data = new DataInputStream(in)) {
            int magic = data.readInt();
            if (magic != 0xCAFEBABE) {
                fail("Not a class file: " + entry.getName());
            }
            data.readUnsignedShort(); // minor version
            return data.readUnsignedShort();
        }
    }

    private static Path locateJar() {
        String configured = System.getProperty("otel.extension.jar");
        if (configured == null) {
            fail("otel.extension.jar system property is not set; see the test task in build.gradle.kts");
        }
        Path jar = Path.of(configured);
        if (!Files.isRegularFile(jar)) {
            fail("Extension jar not found at " + jar);
        }
        return jar;
    }
}
