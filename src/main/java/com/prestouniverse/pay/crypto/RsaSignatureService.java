package com.prestouniverse.pay.crypto;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.util.Base64;

/** SHA256withRSA signing and verification over a canonical string, Base64-encoded as the gateway expects. */
public final class RsaSignatureService {

    private static final String ALGORITHM = "SHA256withRSA";

    private RsaSignatureService() {
    }

    public static String sign(String canonical, PrivateKey privateKey) {
        try {
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to sign canonical string", e);
        }
    }

    public static boolean verify(String canonical, String signatureBase64, PublicKey publicKey) {
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(signatureBase64);
            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(canonical.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            return false;
        }
    }
}
