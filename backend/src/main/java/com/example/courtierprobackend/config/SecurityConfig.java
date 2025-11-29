package com.example.courtierprobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource())) // <-- REQUIRED FOR CORS TO WORK

                .authorizeHttpRequests(auth -> auth
                                .requestMatchers("/actuator/health").permitAll()
                                .requestMatchers("/actuator/health/**").permitAll()

                                .requestMatchers("/actuator/**").denyAll()

                                // pour l'instant toutes les autres routes restent publiques
                                .anyRequest().permitAll()
                        // .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()));

        return http.build();
    }

    // CORS configuration that Spring Security ACTUALLY uses
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(List.of(
                "http://localhost:5173",    // frontend dev
                "http://127.0.0.1:5173",    // optional
                "http://localhost:8080",    // backend local
                "http://localhost:5174",     // <-- ADD THIS
                "https://courtierproapp.sraldon.work",   // prod FE
                "https://courtierproapi.sraldon.work"    // prod BE
        ));

        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
