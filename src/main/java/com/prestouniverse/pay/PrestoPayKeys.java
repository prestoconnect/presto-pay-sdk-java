package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayConfigException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Loads the merchant private key (PKCS#12) and the Presto public key (X.509 certificate).
 */
public final class PrestoPayKeys {

    private PrestoPayKeys() {
    }

    /**
     * Loads the only private key in a PKCS#12 keystore.
     *
     * @throws PrestoPayConfigException if the keystore cannot be read or does not hold exactly one private key
     */
    public static PrivateKey privateKeyFromPkcs12(InputStream in, char[] password) {
        return privateKeyFromPkcs12(in, password, null);
    }

    /**
     * Loads a private key from a PKCS#12 keystore.
     *
     * @param alias key alias, or {@code null} to use the keystore's only private key
     * @throws PrestoPayConfigException if the keystore cannot be read or the alias is not a private key
     */
    public static PrivateKey privateKeyFromPkcs12(InputStream in, char[] password, String alias) {
        if (in == null) {
            throw new PrestoPayConfigException("keystore", "keystore input stream must not be null");
        }
        try {
            KeyStore keyStore = KeyStore.getInstance("PKCS12");
            keyStore.load(in, password);
            String resolvedAlias = alias != null ? alias : singleKeyAlias(keyStore);
            Key key = keyStore.isKeyEntry(resolvedAlias) ? keyStore.getKey(resolvedAlias, password) : null;
            if (!(key instanceof PrivateKey)) {
                throw new PrestoPayConfigException("alias", "No private key found for alias '" + resolvedAlias + "'");
            }
            return (PrivateKey) key;
        } catch (IOException | GeneralSecurityException e) {
            throw new PrestoPayConfigException("keystore", "Failed to load PKCS12 keystore", e);
        }
    }

    /**
     * Loads the only private key in a PKCS#12 keystore file.
     *
     * @throws PrestoPayConfigException if the file cannot be read or does not hold exactly one private key
     */
    public static PrivateKey privateKeyFromPkcs12(Path path, char[] password) {
        return privateKeyFromPkcs12(path, password, null);
    }

    /**
     * Loads a private key from a PKCS#12 keystore file.
     *
     * @param alias key alias, or {@code null} to use the keystore's only private key
     * @throws PrestoPayConfigException if the file cannot be read or the alias is not a private key
     */
    public static PrivateKey privateKeyFromPkcs12(Path path, char[] password, String alias) {
        try (InputStream in = Files.newInputStream(path)) {
            return privateKeyFromPkcs12(in, password, alias);
        } catch (IOException e) {
            throw new PrestoPayConfigException("keystore", "Failed to read keystore file: " + path, e);
        }
    }

    /**
     * Loads the public key from an X.509 certificate (PEM or DER).
     */
    public static PublicKey publicKeyFromX509(InputStream in) {
        if (in == null) {
            throw new PrestoPayConfigException("prestoPublicKey", "certificate input stream must not be null");
        }
        try {
            CertificateFactory factory = CertificateFactory.getInstance("X.509");
            Certificate certificate = factory.generateCertificate(in);
            return certificate.getPublicKey();
        } catch (GeneralSecurityException e) {
            throw new PrestoPayConfigException("prestoPublicKey", "Failed to load X.509 certificate", e);
        }
    }

    /**
     * Loads the public key from an X.509 certificate file (PEM or DER).
     */
    public static PublicKey publicKeyFromX509(Path path) {
        try (InputStream in = Files.newInputStream(path)) {
            return publicKeyFromX509(in);
        } catch (IOException e) {
            throw new PrestoPayConfigException("prestoPublicKey", "Failed to read certificate file: " + path, e);
        }
    }

    private static String singleKeyAlias(KeyStore keyStore) throws GeneralSecurityException {
        List<String> keyAliases = new ArrayList<>();
        for (String alias : Collections.list(keyStore.aliases())) {
            if (keyStore.isKeyEntry(alias)) {
                keyAliases.add(alias);
            }
        }
        if (keyAliases.size() != 1) {
            throw new PrestoPayConfigException("alias",
                    "Keystore holds " + keyAliases.size() + " private keys; specify an alias");
        }
        return keyAliases.get(0);
    }
}
