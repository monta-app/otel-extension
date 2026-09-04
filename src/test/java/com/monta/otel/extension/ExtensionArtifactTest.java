package com.monta.otel.extension;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
 * A jar compiled for a newer runtime than its consumer throws {@code UnsupportedClassVersionError}
 * in premain: the OpenTelemetry Java agent aborts and the JVM keeps serving, uninstrumented and
 * without an error.
 */
class ExtensionArtifactTest {

    /** The oldest runtime any consumer runs. Deriving this from the build would make the check circular. */
    private static final int OLDEST_SUPPORTED_JAVA_RELEASE = 21;

    /** Class file major version is the Java release plus 44: Java 21 is 65. */
    private static final int CLASS_FILE_MAJOR_OFFSET = 44;

    // Bytecode here is only loaded by runtimes new enough to read it, so it is not a premain hazard.
    private static final String MULTI_RELEASE_PREFIX = "META-INF/versions/";

    @Test
    void publishedJarRunsOnTheOldestSupportedRuntime() throws IOException {
        Path jar = locateJar();
        int maxMajor = OLDEST_SUPPORTED_JAVA_RELEASE + CLASS_FILE_MAJOR_OFFSET;

        List<String> tooNew = new ArrayList<>();
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (!name.endsWith(".class") || name.startsWith(MULTI_RELEASE_PREFIX)) {
                    continue;
                }
                int major = readClassFileMajorVersion(jarFile, entry);
                if (major > maxMajor) {
                    tooNew.add(name + " (class file major " + major + ")");
                }
            }
        }

        assertTrue(
                tooNew.isEmpty(),
                "These classes, including bundled dependencies, target a newer runtime than the"
                        + " oldest consumer (max major "
                        + maxMajor
                        + "): "
                        + tooNew);
    }

    @Test
    void buildTargetsTheOldestSupportedRuntime() {
        assertEquals(
                OLDEST_SUPPORTED_JAVA_RELEASE,
                releaseTarget(),
                "options.release must match the oldest runtime consumers run, otherwise the bytecode"
                        + " check above would move with it and stop guarding anything");
    }

    @Test
    void jarContainsTheExtensionItself() throws IOException {
        try (JarFile jarFile = new JarFile(locateJar().toFile())) {
            assertTrue(
                    jarFile.stream().anyMatch(e -> e.getName().equals("com/monta/otel/extension/Customizer.class")),
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

    private static int releaseTarget() {
        String configured = System.getProperty("otel.extension.release");
        if (configured == null) {
            fail("otel.extension.release system property is not set; see the test task in build.gradle.kts");
        }
        return Integer.parseInt(configured);
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
