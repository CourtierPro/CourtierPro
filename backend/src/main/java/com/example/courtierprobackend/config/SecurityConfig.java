package com.example.courtierprobackend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.DefaultAuthenticationEventPublisher;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.ArrayList;
import java.util.Collection;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true) // to be able to use @PreAuthorize on controllers
public class SecurityConfig {

    private final CorsConfigurationSource corsConfigurationSource;

    public SecurityConfig(CorsConfigurationSource corsConfigurationSource) {
        this.corsConfigurationSource = corsConfigurationSource;
    }

    // MUST match the name of the claim you put in the Auth0 Action
    private static final String ROLES_CLAIM = "https://courtierpro.dev/roles";

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))

                .csrf(csrf -> csrf.disable())

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()

                        .requestMatchers("/actuator/**").denyAll()

                        .requestMatchers(HttpMethod.POST,  "/api/admin/users").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/admin/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET,   "/api/admin/users").hasRole("ADMIN")

                        .requestMatchers("/transactions/**").hasRole("BROKER")


                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );

        return http.build();
    }


    /**
     * This bean tells Spring Security how to extract user roles from our Auth0 JWT.
     *
     * In our case, roles are inside a custom claim: "https://courtierpro.dev/roles"
     *
     * We convert:
     *   "ADMIN" -> "ROLE_ADMIN"
     *   "BROKER" -> "ROLE_BROKER"
     *   "CLIENT" -> "ROLE_CLIENT"
     *
     * so that hasRole("ADMIN") / hasRole("BROKER") work correctly.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(this::extractAuthoritiesFromJwt);
        return converter;
    }

    /**
     * Extracts roles from our Auth0 custom claim and maps them to Spring authorities.
     *
     * Example:
     *   Auth0 JWT contains:
     *     "https://courtierpro.dev/roles": ["ADMIN"]
     *
     *   This method produces:
     *     [ new SimpleGrantedAuthority("ROLE_ADMIN") ]
     *
     * Spring Security will then understand:
     *   hasRole("ADMIN")  --> true
     */
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
