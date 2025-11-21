package com.beratcan.app.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true) // ✅ @PreAuthorize kullanımı için
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPointJwt unauthorizedHandler;

    @Value("${allowed.origins}")
    private String allowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthEntryPointJwt unauthorizedHandler) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS konfigürasyonu
                .csrf(csrf -> csrf.disable()) // CSRF kapalı (REST API için gerekli değil)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedHandler)) // ✅ Profesyonel error handling
                .authorizeHttpRequests(auth -> auth
                        // Swagger UI endpoints (herkese açık - development için)
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        // Public endpoints
                        .requestMatchers("/api/users/register", "/api/users/login").permitAll() // herkes erişebilir
                        .requestMatchers("/api/users/refresh").permitAll() // ✅ Token yenileme herkese açık
                        .requestMatchers("/api/books/store").permitAll() // Kitap mağazası herkese açık
                        .anyRequest().authenticated() // ✅ DİĞER TÜM ENDPOINT'LER AUTH GEREKTİRİR!
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)); // JWT
                                                                                                               // stateless

        // JWT filter ekle
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * NOT: Spring Security 6.3+ versiyonlarında DaoAuthenticationProvider bean'i
     * manuel olarak oluşturmaya gerek YOK!
     * 
     * Spring Security otomatik olarak şunları yapar:
     * 1. UserDetailsService bean'ini bulur
     * 2. PasswordEncoder bean'ini bulur
     * 3. Arka planda DaoAuthenticationProvider oluşturur
     * 
     * Bu nedenle sadece UserDetailsService ve PasswordEncoder bean'lerini
     * tanımlamak yeterlidir (aşağıda tanımlı).
     */

    /**
     * CORS Configuration
     * Production'da sadece belirli origin'lere izin ver
     * application.properties'den allowed.origins okunur
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ Sadece belirlediğimiz origin'lere izin ver (wildcard kullanma!)
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));

        // İzin verilen HTTP metodları
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // İzin verilen header'lar
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // Credentials (cookies, authorization headers) izni
        configuration.setAllowCredentials(true);

        // Preflight cache süresi (1 saat)
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * Password encoder bean
     * BCrypt kullanarak şifreleri hashler
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
