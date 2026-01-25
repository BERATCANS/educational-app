package com.beratcan.app;

import com.beratcan.app.config.DotEnvConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppApplication {

    public static void main(String[] args) {
        // 1. Önce .env dosyasını kontrol et ve profili belirle (Spring Boot başlamadan önce)
        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure().ignoreIfMissing().load();
            String envProfile = dotenv.get("SPRING_PROFILES_ACTIVE");

            if (envProfile != null && !envProfile.trim().isEmpty()) {
                System.setProperty("spring.profiles.active", envProfile);
                System.out.println("AppApplication: Set profile to '" + envProfile + "' from .env");
            } else {
                // Eğer environment variable veya system property yoksa, varsayılan olarak dev yap
                if (System.getenv("SPRING_PROFILES_ACTIVE") == null && System.getProperty("spring.profiles.active") == null) {
                    System.setProperty("spring.profiles.active", "dev");
                    System.out.println("AppApplication: Profile not set, defaulting to 'dev'");
                }
            }
        } catch (Exception e) {
            System.err.println("AppApplication: Error loading .env: " + e.getMessage());
        }

        SpringApplication app = new SpringApplication(AppApplication.class);
        // .env dosyasını Spring Boot başlamadan önce yükle
        app.addInitializers(new DotEnvConfig());
        app.run(args);
    }

}
