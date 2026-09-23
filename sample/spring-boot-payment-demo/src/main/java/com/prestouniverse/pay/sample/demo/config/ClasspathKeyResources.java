package com.prestouniverse.pay.sample.demo.config;

import com.prestouniverse.pay.PrestoPayKeys;
import com.prestouniverse.pay.exception.PrestoPayConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.PublicKey;

final class ClasspathKeyResources {

    private static final String CLASSPATH_PREFIX = "classpath:";

    private ClasspathKeyResources() {
    }

    static PrivateKey privateKeyFromPkcs12(String location, char[] password, String alias) {
        if (isClasspath(location)) {
            try (InputStream in = openClasspath(location)) {
                return PrestoPayKeys.privateKeyFromPkcs12(in, password, alias);
            } catch (IOException e) {
                throw new PrestoPayConfigException("keystore", "Failed to read classpath keystore: " + location, e);
            }
        }
        return PrestoPayKeys.privateKeyFromPkcs12(resolveFilePath(location), password, alias);
    }

    static PublicKey publicKeyFromX509(String location) {
        if (isClasspath(location)) {
            try (InputStream in = openClasspath(location)) {
                return PrestoPayKeys.publicKeyFromX509(in);
            } catch (IOException e) {
                throw new PrestoPayConfigException("prestoPublicKey",
                        "Failed to read classpath certificate: " + location, e);
            }
        }
        return PrestoPayKeys.publicKeyFromX509(resolveFilePath(location));
    }

    private static boolean isClasspath(String location) {
        return location != null && location.startsWith(CLASSPATH_PREFIX);
    }

    private static InputStream openClasspath(String location) {
        String resource = location.substring(CLASSPATH_PREFIX.length());
        if (resource.startsWith("/")) {
            resource = resource.substring(1);
        }
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new PrestoPayConfigException("keystore",
                    "Classpath resource not found: " + resource + " (from " + location + ")");
        }
        return in;
    }

    private static Path resolveFilePath(String path) {
        Path p = Paths.get(path);
        if (p.isAbsolute()) {
            return p;
        }
        return Paths.get(System.getProperty("user.dir")).resolve(p).normalize();
    }
}
