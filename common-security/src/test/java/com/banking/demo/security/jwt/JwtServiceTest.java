package com.banking.demo.security.jwt;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long expirationMs = 3600000; // 1 hora

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    @DisplayName("Debe generar token y extraer subject correctamente")
    void shouldGenerateAndExtractSubject() {
        String token = jwtService.generateToken("1");

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractSubject(token)).isEqualTo("1");
        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.isTokenValid(token, "1")).isTrue();
        assertThat(jwtService.isTokenValid(token, "OTHER")).isFalse();
    }

    @Test
    @DisplayName("Debe incluir y extraer claims personalizados: identificacion y roles")
    void shouldIncludeAndExtractCustomClaims() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("identificacion", "098254785");
        claims.put("roles", List.of("ROLE_USER", "ROLE_ADMIN"));

        String token = jwtService.generateToken("1", claims);

        assertThat(jwtService.extractSubject(token)).isEqualTo("1");
        assertThat(jwtService.extractIdentificacion(token)).isEqualTo("098254785");
        assertThat(jwtService.extractRoles(token)).containsExactly("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("Debe retornar falso cuando el token es invalido o corrupto")
    void shouldReturnFalseForInvalidToken() {
        assertThat(jwtService.isTokenValid("invalid.token.here")).isFalse();
    }

    @Test
    @DisplayName("Debe retornar falso cuando el token esta expirado")
    void shouldReturnFalseForExpiredToken() {
        JwtService expiredJwtService = new JwtService(secret, -1000); // Ya expirado
        String token = expiredJwtService.generateToken("1");

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
