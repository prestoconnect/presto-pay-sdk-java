package com.prestouniverse.pay.sample.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "prestopay")
public class PrestoPayProperties {

    /** {@code staging} or {@code production}. */
    private String environment = "staging";

    private String mid = "11StreetMock";

    private String mrn = "PM181019QGJWH4K";

    /** Partner PKCS#12 for request signing ({@code classpath:keys/...} or filesystem path). */
    private String keystorePath = "classpath:keys/presto_rm_keystore.p12";

    private String keystorePassword = "123123123";

    private String keystoreAlias = "rm";

    /** Presto X.509 public key (DER) for response and webhook verification. */
    private String publicKeyPath = "classpath:keys/presto_ext_service_dev.der";

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getMid() {
        return mid;
    }

    public void setMid(String mid) {
        this.mid = mid;
    }

    public String getMrn() {
        return mrn;
    }

    public void setMrn(String mrn) {
        this.mrn = mrn;
    }

    public String getKeystorePath() {
        return keystorePath;
    }

    public void setKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
    }

    public String getKeystorePassword() {
        return keystorePassword;
    }

    public void setKeystorePassword(String keystorePassword) {
        this.keystorePassword = keystorePassword;
    }

    public String getKeystoreAlias() {
        return keystoreAlias;
    }

    public void setKeystoreAlias(String keystoreAlias) {
        this.keystoreAlias = keystoreAlias;
    }

    public String getPublicKeyPath() {
        return publicKeyPath;
    }

    public void setPublicKeyPath(String publicKeyPath) {
        this.publicKeyPath = publicKeyPath;
    }
}
