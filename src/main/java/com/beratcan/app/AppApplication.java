package com.beratcan.app;

import com.beratcan.app.config.DotEnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AppApplication.class);
        // .env dosyasını Spring Boot başlamadan önce yükle
        app.addInitializers(new DotEnvConfig());
        app.run(args);
    }

}
