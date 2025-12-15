package com.beratcan.app.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson Configuration
 * JSON serialization/deserialization ayarları:
 * - Java 8 date/time support (LocalDate, LocalTime, LocalDateTime)
 * - ISO-8601 date format (timestamp yerine)
 * - Null field'ları JSON'dan çıkarır
 * - Bilinmeyen JSON field'larında hata vermez (API versioning için)
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();

        // Java 8 date/time support
        mapper.registerModule(new JavaTimeModule());

        // Do not fail on unknown JSON fields (API versioning için önemli)
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        // Dates as ISO-8601, not timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Ignore null fields in JSON output
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        return mapper;
    }
}
