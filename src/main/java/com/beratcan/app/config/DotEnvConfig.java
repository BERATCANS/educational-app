package com.beratcan.app.config;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvEntry;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

/**
 * .env dosyasını okumak ve environment variable'lara yüklemek için
 * configuration
 * Spring Boot başlatılmadan önce .env dosyasındaki değerleri System
 * properties'e yükler
 */
@Slf4j
@Configuration
public class DotEnvConfig {

    @PostConstruct
    public void loadDotEnv() {
        try {
            // .env dosyasını oku
            Dotenv dotenv = Dotenv.configure()
                    .ignoreIfMissing() // .env dosyası yoksa hata verme
                    .load();

            // .env dosyasındaki tüm değerleri System properties'e yükle
            // Bu sayede Spring Boot ${VAR_NAME} formatındaki değerleri okuyabilir
            Set<DotenvEntry> envVars = dotenv.entries();

            if (!envVars.isEmpty()) {
                log.info(".env dosyası bulundu ve yüklendi. {} değişken yüklendi.", envVars.size());

                envVars.forEach(entry -> {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    // Eğer System property'de yoksa, .env'den yükle
                    // Bu sayede environment variable'lar .env dosyasından önceliklidir
                    if (System.getProperty(key) == null && System.getenv(key) == null) {
                        System.setProperty(key, value);
                        log.debug("Environment variable yüklendi: {} (değer gizlendi)", key);
                    }
                });
            } else {
                log.warn(".env dosyası bulunamadı veya boş. Environment variable'lar kullanılacak.");
            }
        } catch (Exception e) {
            log.warn(".env dosyası yüklenirken hata oluştu: {}. Environment variable'lar kullanılacak.",
                    e.getMessage());
        }
    }
}
