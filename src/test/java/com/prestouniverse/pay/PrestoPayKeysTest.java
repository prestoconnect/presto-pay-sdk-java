package com.prestouniverse.pay;

import com.prestouniverse.pay.exception.PrestoPayConfigException;
import com.prestouniverse.pay.support.TestKeys;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrestoPayKeysTest {

    private static final char[] PASSWORD = TestKeys.KEYSTORE_PASSWORD.toCharArray();

    @Test
    void singlePrivateKeyIsFoundWithoutAnAlias() throws Exception {
        PrivateKey key = PrestoPayKeys.privateKeyFromPkcs12(keystore(false, true), PASSWORD);

        assertArrayEquals(TestKeys.privateKey().getEncoded(), key.getEncoded());
    }

    @Test
    void aliasIsRequiredWhenTheKeystoreHoldsSeveralPrivateKeys() throws Exception {
        PrestoPayConfigException error = assertThrows(PrestoPayConfigException.class,
                () -> PrestoPayKeys.privateKeyFromPkcs12(keystore(true, false), PASSWORD));
        assertEquals("alias", error.field());

        PrivateKey key = PrestoPayKeys.privateKeyFromPkcs12(keystore(true, false), PASSWORD, "second");
        assertArrayEquals(TestKeys.privateKey().getEncoded(), key.getEncoded());
    }

    @Test
    void aliasThatIsNotAPrivateKeyIsAConfigError() throws Exception {
        assertEquals("alias", assertThrows(PrestoPayConfigException.class,
                () -> PrestoPayKeys.privateKeyFromPkcs12(keystore(false, true), PASSWORD, "cert-only")).field());
        assertEquals("alias", assertThrows(PrestoPayConfigException.class,
                () -> PrestoPayKeys.privateKeyFromPkcs12(keystore(false, false), PASSWORD, "missing")).field());
    }

    @Test
    void wrongPasswordIsAConfigError() throws Exception {
        assertEquals("keystore", assertThrows(PrestoPayConfigException.class,
                () -> PrestoPayKeys.privateKeyFromPkcs12(keystore(false, false), "wrong".toCharArray())).field());
    }

    private static InputStream keystore(boolean secondKey, boolean certificateEntry) throws Exception {
        KeyStore source = KeyStore.getInstance("PKCS12");
        try (InputStream in = PrestoPayKeysTest.class.getClassLoader().getResourceAsStream("keys/test-keystore.p12")) {
            source.load(in, PASSWORD);
        }
        Certificate[] chain = source.getCertificateChain(TestKeys.KEYSTORE_ALIAS);

        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, PASSWORD);
        keyStore.setKeyEntry(TestKeys.KEYSTORE_ALIAS, TestKeys.privateKey(), PASSWORD, chain);
        if (secondKey) {
            keyStore.setKeyEntry("second", TestKeys.privateKey(), PASSWORD, chain);
        }
        if (certificateEntry) {
            keyStore.setCertificateEntry("cert-only", chain[0]);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        keyStore.store(out, PASSWORD);
        return new ByteArrayInputStream(out.toByteArray());
    }
}
