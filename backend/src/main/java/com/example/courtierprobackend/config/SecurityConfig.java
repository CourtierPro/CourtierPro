package com.example.courtierprobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // CI/CD, load balancers, uptime monitors
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/actuator/health/**").permitAll()

                        // everything else in actuator secure
                        .requestMatchers("/actuator/**").denyAll()

                        // your public APIs
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
