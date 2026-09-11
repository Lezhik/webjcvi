package org.webjcvi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class WebJcviApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebJcviApplication.class, args);
    }
}
