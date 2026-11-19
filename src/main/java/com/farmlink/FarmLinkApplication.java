package com.farmlink;

import com.farmlink.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(AppProperties.class)
public class FarmLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(FarmLinkApplication.class, args);
    }
}
