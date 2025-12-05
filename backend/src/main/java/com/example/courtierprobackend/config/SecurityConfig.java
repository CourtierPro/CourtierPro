package com.example.courtierprobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.Customizer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    // Must match the name of the claim you put in the Auth0 Action
    private static final String ROLES_CLAIM = "https://courtierpro.dev/roles";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        .requestMatchers("/actuator/**").denyAll()

                        // Admin user management
                        .requestMatchers(HttpMethod.POST, "/api/admin/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/admin/users").hasRole("ADMIN")

                        //  Admin organization settings
                        .requestMatchers("/api/admin/settings/**").hasRole("ADMIN")

                        // Broker-only transaction APIs
                        .requestMatchers("/transactions/**").hasRole("BROKER")

                        // Everything else must be authenticated
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthoritiesFromJwt);
        return converter;
    }

    private Collection<GrantedAuthority> extractAuthoritiesFromJwt(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        Object rolesObj = jwt.getClaim(ROLES_CLAIM);

        if (rolesObj instanceof Collection<?> roles) {
            for (Object role : roles) {
                String roleName = String.valueOf(role);
                authorities.add(new SimpleGrantedAuthority("ROLE_" + roleName));
            }
        }

        return authorities;
    }

    @Bean
    public DefaultAuthenticationEventPublisher authenticationEventPublisher() {
        return new DefaultAuthenticationEventPublisher();
    }
}
