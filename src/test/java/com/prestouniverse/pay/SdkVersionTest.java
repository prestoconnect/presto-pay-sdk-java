package com.prestouniverse.pay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SdkVersionTest {

    @Test
    void versionIsNotEmptyAndNotDevInMavenBuild() {
        String version = SdkVersion.version();
        assertFalse(version.isEmpty());
        assertTrue(version.contains("0.1.0"), "expected Maven-filtered version, got: " + version);
    }
}
