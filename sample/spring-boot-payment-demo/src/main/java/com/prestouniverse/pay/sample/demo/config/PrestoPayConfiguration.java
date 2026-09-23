package com.prestouniverse.pay.sample.demo.config;

import com.prestouniverse.pay.Environment;
import com.prestouniverse.pay.PrestoPayClient;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PrestoPayConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PrestoPayConfiguration.class);

    @Bean(destroyMethod = "")
    PrestoPayClient prestoPayClient(PrestoPayProperties properties) {
        PrestoPayClient client;
        if (useEnvironmentVariables()) {
            log.info("Creating PrestoPayClient from PRESTOPAY_* environment variables");
            client = PrestoPayClient.fromEnv();
        } else {
            log.info("Creating PrestoPayClient from application config: env={} mid={} prestoMrn={} "
                            + "keystore={} publicKey={} keystoreAlias={}",
                    properties.getEnvironment(),
                    properties.getMid(),
                    properties.getMrn(),
                    properties.getKeystorePath(),
                    properties.getPublicKeyPath(),
                    properties.getKeystoreAlias());
            client = PrestoPayClient.builder()
                    .environment(parseEnvironment(properties.getEnvironment()))
                    .merchantId(properties.getMid())
                    .merchantRefNum(properties.getMrn())
                    .privateKey(ClasspathKeyResources.privateKeyFromPkcs12(
                            properties.getKeystorePath(),
                            properties.getKeystorePassword().toCharArray(),
                            properties.getKeystoreAlias()))
                    .prestoPublicKey(ClasspathKeyResources.publicKeyFromX509(properties.getPublicKeyPath()))
                    .build();
        }
        return client;
    }

    /**
     * When full {@code PRESTOPAY_*} env is set, defer to the SDK (e.g. CI or production-like runs).
     */
    private static boolean useEnvironmentVariables() {
        return System.getenv("PRESTOPAY_KEYSTORE_PATH") != null
                && System.getenv("PRESTOPAY_KEYSTORE_PASSWORD") != null
                && System.getenv("PRESTOPAY_KEYSTORE_ALIAS") != null
                && System.getenv("PRESTOPAY_PUBLIC_KEY_PATH") != null;
    }

    private static Environment parseEnvironment(String name) {
        if ("staging".equalsIgnoreCase(name)) {
            return Environment.STAGING;
        }
        if ("production".equalsIgnoreCase(name)) {
            return Environment.PRODUCTION;
        }
        throw new PrestoPayConfigException("prestopay.environment", "Unknown environment: " + name);
    }
}
