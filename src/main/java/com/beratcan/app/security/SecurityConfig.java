package com.beratcan.app.security;

import com.beratcan.app.constants.AppConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true) // ✅ @PreAuthorize kullanımı için
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final AuthEntryPointJwt unauthorizedHandler;
    private final RateLimitFilter rateLimitFilter;

    @Value("${allowed.origins}")
    private String allowedOrigins;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    public SecurityConfig(@Lazy JwtAuthenticationFilter jwtAuthenticationFilter,
            AuthEntryPointJwt unauthorizedHandler,
            RateLimitFilter rateLimitFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.unauthorizedHandler = unauthorizedHandler;
        this.rateLimitFilter = rateLimitFilter;
    }

    /**
     * SecurityContext'i async methodlara propagate et
     * SSE gibi async operasyonlar için gerekli
     */
    @PostConstruct
    public void enableSecurityContextInheritance() {
        SecurityContextHolder.setStrategyName(SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // CORS konfigürasyonu
                .csrf(csrf -> csrf.disable()) // CSRF kapalı (REST API için gerekli değil)
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedHandler)) // ✅ Profesyonel error handling
                .authorizeHttpRequests(auth -> {
                    // Swagger UI endpoints - Production'da sadece admin, dev'de herkese açık
                    if ("prod".equals(activeProfile)) {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                                .hasRole("ADMIN");
                    } else {
                        auth.requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                                .permitAll();
                    }
                    // Public endpoints
                    auth
                            .requestMatchers("/actuator/health").permitAll() // ✅ Health check endpoint - DigitalOcean
                                                                             // ve monitoring için
                            .requestMatchers("/api/users/register", "/api/users/login").permitAll() // herkes erişebilir
                            // /api/users/refresh artık authenticated (refresh token ile authenticate
                            // edilir)
                            .requestMatchers("/api/users/verify-email")
                            .permitAll() // ✅ Email doğrulama herkese açık (email verification token ile güvenlik
                                         // sağlanır)
                            // /api/users/resend-verification ve /api/users/change-email artık authenticated
                            // (ProfileController'da)
                            .requestMatchers("/api/books/store").permitAll() // Kitap mağazası herkese açık
                            .requestMatchers("/api/users/cities").permitAll() // Şehir listesi herkese açık
                            .requestMatchers("/api/files/**").permitAll() // ✅ Dosyalara (Görseller) herkese açık erişim
                            .anyRequest().authenticated(); // ✅ DİĞER TÜM ENDPOINT'LER AUTH GEREKTİRİR!
                })
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // JWT stateless
                // SecurityContext request attribute'da saklansın → async dispatch (SSE bittiğinde) aynı request'te context geri yüklensin
                .securityContext(sec -> sec
                        .securityContextRepository(requestAttributeSecurityContextRepository())
                        .requireExplicitSave(true));

        // Rate limit filter ÖNCE ekle (tüm requestleri kontrol et)
        http.addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);

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

        if ("prod".equals(activeProfile)) {
            // Production: Sadece belirlediğimiz origin'lere izin ver
            configuration.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
            configuration.setAllowCredentials(true);
        } else {
            // Development: Tüm origin'lere izin ver (null origin dahil - file:// için)
            // setAllowedOriginPatterns kullanarak null origin'i de kapsar
            configuration.setAllowedOriginPatterns(Arrays.asList("*"));
            configuration.setAllowCredentials(false); // null origin ile credentials kullanılamaz
        }

        // İzin verilen HTTP metodları
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));

        // İzin verilen header'lar (spesifik - güvenlik için)
        configuration.setAllowedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "X-Requested-With",
                "Accept",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"));

        // Preflight cache süresi (1 saat)
        configuration.setMaxAge(AppConstants.CORS_MAX_AGE_SECONDS);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    /**
     * Request attribute'da SecurityContext saklar; async dispatch (SSE tamamlanınca) aynı request'te context geri yüklenir.
     * @Lazy JwtAuthenticationFilter ile circular dependency kırıldığı için bean burada tanımlanabilir.
     */
    @Bean
    public RequestAttributeSecurityContextRepository requestAttributeSecurityContextRepository() {
        return new RequestAttributeSecurityContextRepository();
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
