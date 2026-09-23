package com.prestouniverse.pay;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class SdkVersion {

    private static final String VERSION = resolveVersion();

    private SdkVersion() {
    }

    /**
     * Returns the SDK version from the JAR manifest or bundled properties, or {@code dev} if unknown.
     */
    public static String version() {
        return VERSION;
    }

    private static String resolveVersion() {
        Package pkg = PrestoPayClient.class.getPackage();
        if (pkg != null) {
            String implementationVersion = pkg.getImplementationVersion();
            if (implementationVersion != null && !implementationVersion.isEmpty()) {
                return implementationVersion;
            }
        }
        try (InputStream in = PrestoPayClient.class.getResourceAsStream("/presto-pay-sdk-version.properties")) {
            if (in != null) {
                Properties properties = new Properties();
                properties.load(in);
                String version = properties.getProperty("version");
                if (version != null && !version.isEmpty()) {
                    return version;
                }
            }
        } catch (IOException e) {
            // ignore: fall back to "dev"
        }
        return "dev";
    }
}
