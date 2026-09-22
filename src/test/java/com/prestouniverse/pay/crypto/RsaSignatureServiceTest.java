package com.prestouniverse.pay.crypto;

import com.prestouniverse.pay.support.TestKeys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RsaSignatureServiceTest {

    @Test
    void signatureVerifiesAgainstTheMatchingPublicKey() {
        String canonical = "1200:MYR:Order #12345:PW2401XH9KCX";

        String signature = RsaSignatureService.sign(canonical, TestKeys.privateKey());

        assertTrue(RsaSignatureService.verify(canonical, signature, TestKeys.publicKey()));
    }

    @Test
    void tamperedCanonicalStringFailsVerification() {
        String canonical = "1200:MYR:Order #12345:PW2401XH9KCX";
        String tampered = "9999:MYR:Order #12345:PW2401XH9KCX";

        String signature = RsaSignatureService.sign(canonical, TestKeys.privateKey());

        assertFalse(RsaSignatureService.verify(tampered, signature, TestKeys.publicKey()));
    }

    @Test
    void chineseCharactersInTheCanonicalStringSignAndVerifyCorrectly() {
        String canonical = "1200:MYR:訂單 #12345 測試:PW2401XH9KCX";

        String signature = RsaSignatureService.sign(canonical, TestKeys.privateKey());

        assertTrue(RsaSignatureService.verify(canonical, signature, TestKeys.publicKey()));
    }

    @Test
    void multiLineCanonicalStringSignsAndVerifiesCorrectly() {
        String canonical = "PW2401XH9KCX:line one\r\nline two\r\nline three";

        String signature = RsaSignatureService.sign(canonical, TestKeys.privateKey());

        assertTrue(RsaSignatureService.verify(canonical, signature, TestKeys.publicKey()));
    }

    @Test
    void malformedBase64FailsVerificationRatherThanThrowing() {
        assertFalse(RsaSignatureService.verify("anything", "not-valid-base64!!!", TestKeys.publicKey()));
    }
}
