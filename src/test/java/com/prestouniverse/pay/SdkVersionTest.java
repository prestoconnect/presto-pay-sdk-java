package com.prestouniverse.pay;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SdkVersionTest {

    @Test
    void versionIsNotEmptyAndNotDevInMavenBuild() {
        String version = SdkVersion.version();
        assertFalse(version.isEmpty());
        assertNotEquals("dev", version, "expected Maven-filtered version, got: " + version);
        assertNotEquals("@project.version@", version, "Maven resource filtering did not run");
    }
}
