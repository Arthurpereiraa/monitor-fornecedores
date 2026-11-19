package io.github.arthurpereiraa.monitor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MonitorFornecedoresApplication {

    public static void main(String[] args) {
        SpringApplication.run(MonitorFornecedoresApplication.class, args);
    }
}
