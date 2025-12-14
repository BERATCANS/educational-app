package com.beratcan.app.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * .env dosyasını okumak ve environment variable'lara yüklemek için
 * configuration
 * Spring Boot başlatılmadan ÖNCE .env dosyasındaki değerleri System
 * properties'e yükler
 * 
 * ApplicationContextInitializer kullanarak Spring Boot'un property
 * loading'inden
 * önce çalışır
 */
@Slf4j
public class DotEnvConfig implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(@org.springframework.lang.NonNull ConfigurableApplicationContext applicationContext) {
        // Önce .env dosyasını oku (eğer varsa)
        // Production'da da local test için .env okunabilir
        // Ama production deployment'ta environment variables kullanılmalı

        // .env dosyasını oku (local test için production'da da kullanılabilir)
        try {
            // .env dosyasını oku
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // .env dosyası yoksa hata verme
                    .load();

            // .env dosyasındaki tüm değerleri System properties'e yükle
            Set<DotenvEntry> envVars = dotenv.entries();

            if (!envVars.isEmpty()) {
                ConfigurableEnvironment environment = applicationContext.getEnvironment();
                Map<String, Object> dotenvProperties = new HashMap<>();

                // ÖNCE SPRING_PROFILES_ACTIVE'i oku ve set et (diğer değişkenlerden önce)
                String springProfilesActive = dotenv.get("SPRING_PROFILES_ACTIVE");
                if (springProfilesActive != null && !springProfilesActive.trim().isEmpty()) {
                    if (System.getProperty("spring.profiles.active") == null
                            && System.getenv("SPRING_PROFILES_ACTIVE") == null) {
                        System.setProperty("spring.profiles.active", springProfilesActive);
                        dotenvProperties.put("SPRING_PROFILES_ACTIVE", springProfilesActive);
                        log.debug("SPRING_PROFILES_ACTIVE yüklendi: {}", springProfilesActive);
                    }
                }

                // Profile kontrolü (log için) - şimdi doğru değeri görecek
                String activeProfile = System.getProperty("spring.profiles.active");
                if (activeProfile == null) {
                    activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
                }
                String profileInfo = "prod".equals(activeProfile) ? "production mode (local test)" : "development mode";
                log.info(".env dosyası bulundu ve yüklendi. {} değişken yüklendi ({})", envVars.size(), profileInfo);

                // Diğer tüm değişkenleri yükle (SPRING_PROFILES_ACTIVE hariç, zaten yüklendi)
                envVars.forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // SPRING_PROFILES_ACTIVE'i atla (zaten yüklendi)
                    if ("SPRING_PROFILES_ACTIVE".equals(key)) {
                        return;
                    }

                    // Eğer System property veya environment variable'da yoksa, .env'den yükle
                    // Bu sayede environment variable'lar .env dosyasından önceliklidir
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                        dotenvProperties.put(key, value);
                        log.debug("Environment variable yüklendi: {} (değer gizlendi)", key);
                    }
                });

                // Spring Environment'a da ekle (property source olarak)
                if (!dotenvProperties.isEmpty()) {
                    environment.getPropertySources()
                            .addFirst(new MapPropertySource("dotenv", dotenvProperties));
                }
            } else {
                log.warn(".env dosyası bulunamadı veya boş. Environment variable'lar kullanılacak.");
            }
        } catch (Exception e) {
            log.warn(".env dosyası yüklenirken hata oluştu: {}. Environment variable'lar kullanılacak.",
                    e.getMessage());
        }
    }
}
