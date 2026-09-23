package com.prestouniverse.pay.sample.demo;

import com.prestouniverse.pay.sample.demo.config.AppProperties;
import com.prestouniverse.pay.sample.demo.config.PrestoPayProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AppProperties.class, PrestoPayProperties.class})
public class PaymentDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentDemoApplication.class, args);
    }
}
