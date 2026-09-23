package com.prestouniverse.pay.crypto;

import com.prestouniverse.pay.internal.Canonicalization;
import com.prestouniverse.pay.internal.json.JsonObject;

/**
 * Reproduces the gateway canonical string that request, response, and webhook signatures cover. Use it to
 * debug signature mismatches; the SDK signs and verifies automatically.
 */
public final class Canonicalizer {

    private Canonicalizer() {
    }

    /**
     * Builds the gateway canonical string from a JSON request or response body.
     *
     * @param json UTF-8 JSON object text; a {@code signature} field, if present, is excluded
     * @return colon-separated values in ascending key order
     * @throws IllegalArgumentException if {@code json} is not a JSON object or contains unsupported value types
     */
    public static String canonicalizeJson(String json) {
        return Canonicalization.canonicalize(JsonObject.parse(json));
    }
}
