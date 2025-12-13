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
        // Production profile kontrolü - Production'da .env dosyası kullanılmaz
        String activeProfile = System.getProperty("spring.profiles.active");
        if (activeProfile == null) {
            activeProfile = System.getenv("SPRING_PROFILES_ACTIVE");
        }

        // Production'da .env dosyası okunmaz (sadece environment variables kullanılır)
        if ("prod".equals(activeProfile)) {
            log.info("Production profile aktif. .env dosyası okunmayacak, sadece environment variables kullanılacak.");
            return;
        }

        // Development/test ortamlarında .env dosyasını oku
        try {
            // .env dosyasını oku
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // .env dosyası yoksa hata verme
                    .load();

            // .env dosyasındaki tüm değerleri System properties'e yükle
            Set<DotenvEntry> envVars = dotenv.entries();

            if (!envVars.isEmpty()) {
                log.info(".env dosyası bulundu ve yüklendi. {} değişken yüklendi (development mode).", envVars.size());

                ConfigurableEnvironment environment = applicationContext.getEnvironment();
                Map<String, Object> dotenvProperties = new HashMap<>();

                envVars.forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

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
