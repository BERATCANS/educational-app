package com.beratcan.app.config;

import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Dev profilinde Flyway migration stratejisi: önce repair (henüz pushlanmamış
 * migration dosyalarındaki checksum mismatch'lerini DB'de güncelle), sonra migrate.
 * Production'da bu bean yok — orada strict validation devam eder.
 */
@Configuration
@Profile("dev")
public class FlywayDevConfig {

    @Bean
    public FlywayMigrationStrategy repairThenMigrate() {
        return flyway -> {
            flyway.repair();
            flyway.migrate();
        };
    }
}
