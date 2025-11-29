    package com.example.courtierprobackend.config;

    import org.springframework.beans.factory.annotation.Value;
    import org.springframework.context.annotation.Bean;
    import org.springframework.context.annotation.Configuration;
    import org.springframework.security.oauth2.jwt.JwtDecoder;
    import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

    @Configuration
    public class JwtConfig {

        @Value("${auth0.domain}")
        private String domain;

        @Bean
        public JwtDecoder jwtDecoder() {
            String jwkUri = "https://" + domain + "/.well-known/jwks.json";
            return NimbusJwtDecoder.withJwkSetUri(jwkUri).build();
        }
    }
