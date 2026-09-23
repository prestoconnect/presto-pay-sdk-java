package com.prestouniverse.pay.sample.demo.config;

import com.prestouniverse.pay.Environment;
import com.prestouniverse.pay.PrestoPayClient;
import com.prestouniverse.pay.exception.PrestoPayConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

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
            requireSetting("prestopay.mid", "PRESTOPAY_MID", properties.getMid());
            requireSetting("prestopay.mrn", "PRESTOPAY_MRN", properties.getMrn());
            requireSetting("prestopay.keystore-password", "PRESTOPAY_KEYSTORE_PASSWORD",
                    properties.getKeystorePassword());
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
                    .privateKey(ClasspathKeyResources.privateKeyFromPkcs12(
                            properties.getKeystorePath(),
                            properties.getKeystorePassword().toCharArray(),
                            StringUtils.hasText(properties.getKeystoreAlias()) ? properties.getKeystoreAlias() : null))
                    .prestoPublicKey(ClasspathKeyResources.publicKeyFromX509(properties.getPublicKeyPath()))
                    .build();
        }
        return client;
    }

    /**
     * When full {@code PRESTOPAY_*} env is set, defer to the SDK (e.g. CI or production-like runs).
     */
    private static boolean useEnvironmentVariables() {
        return System.getenv("PRESTOPAY_MID") != null
                && System.getenv("PRESTOPAY_KEYSTORE_PATH") != null
                && System.getenv("PRESTOPAY_KEYSTORE_PASSWORD") != null
                && System.getenv("PRESTOPAY_PUBLIC_KEY_PATH") != null;
    }

    private static void requireSetting(String property, String envVar, String value) {
        if (!StringUtils.hasText(value)) {
            throw new PrestoPayConfigException(property,
                    property + " is not set: export " + envVar + " or add it to application-local.yml");
        }
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
