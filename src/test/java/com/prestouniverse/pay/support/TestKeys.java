package com.prestouniverse.pay.support;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public final class TestKeys {

    public static final String KEYSTORE_PASSWORD = "test1234";
    public static final String KEYSTORE_ALIAS = "prestopay-test";

    private TestKeys() {
    }

    public static PrivateKey privateKey() {
        try (InputStream in = open("keys/test-keystore.p12")) {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, KEYSTORE_PASSWORD.toCharArray());
            return (PrivateKey) keyStore.getKey(KEYSTORE_ALIAS, KEYSTORE_PASSWORD.toCharArray());
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to load test keystore", e);
        }
    }

    public static PublicKey publicKey() {
        try (InputStream in = open("keys/test-publickey.der")) {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate certificate = factory.generateCertificate(in);
            return certificate.getPublicKey();
        } catch (IOException | GeneralSecurityException e) {
            throw new IllegalStateException("Failed to load test public key", e);
        }
    }

    private static InputStream open(String resource) {
        InputStream in = TestKeys.class.getClassLoader().getResourceAsStream(resource);
        if (in == null) {
            throw new IllegalStateException("Test resource not found: " + resource);
        }
        return in;
    }
}
