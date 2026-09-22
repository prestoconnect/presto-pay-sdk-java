package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;

public final class PrestoPayKeys {

    private PrestoPayKeys() {
    }

    public static PrivateKey privateKeyFromPkcs12(InputStream in, char[] password, String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, password);
            PrivateKey key = (PrivateKey) keyStore.getKey(alias, password);
            if (key == null) {
                throw new PrestoPayConfigException("alias", "No private key found for alias '" + alias + "'");
            }
            return key;
        } catch (IOException | GeneralSecurityException e) {
            throw new PrestoPayConfigException("keystore", "Failed to load PKCS12 keystore", e);
        }
    }

    public static PrivateKey privateKeyFromPkcs12(Path path, char[] password, String alias) {
        try (InputStream in = Files.newInputStream(path)) {
            return privateKeyFromPkcs12(in, password, alias);
        } catch (IOException e) {
            throw new PrestoPayConfigException("keystore", "Failed to read keystore file: " + path, e);
        }
    }

    public static PublicKey publicKeyFromX509(InputStream in) {
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate certificate = factory.generateCertificate(in);
            return certificate.getPublicKey();
        } catch (GeneralSecurityException e) {
            throw new PrestoPayConfigException("prestoPublicKey", "Failed to load X.509 certificate", e);
        }
    }

    public static PublicKey publicKeyFromX509(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return publicKeyFromX509(in);
        } catch (IOException e) {
            throw new PrestoPayConfigException("prestoPublicKey", "Failed to read certificate file: " + path, e);
        }
    }
}
