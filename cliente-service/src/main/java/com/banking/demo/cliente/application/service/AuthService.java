package com.banking.demo.cliente.application.service;

import com.banking.demo.cliente.domain.model.Cliente;
import com.banking.demo.cliente.domain.repository.ClienteRepository;
import com.banking.demo.security.dto.AuthRequest;
import com.banking.demo.security.dto.AuthResponse;
import com.banking.demo.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse login(AuthRequest request) {
        if (request == null || request.getContrasena() == null || request.getIdentificacion() == null || request.getIdentificacion().isBlank()) {
            log.warn("Intento de login rechazado: credenciales nulas o incompletas");
            throw new BadCredentialsException("Credenciales inválidas");
        }

        String identificacion = request.getIdentificacion().trim();
        log.info("Procesando intento de autenticación para identificación: {}", identificacion);

        Cliente cliente = clienteRepository.findByIdentificacion(identificacion)
                .orElseThrow(() -> {
                    log.warn("Autenticación fallida: cliente no encontrado para identificación [{}]", identificacion);
                    return new BadCredentialsException("Credenciales inválidas");
                });

        if (!passwordEncoder.matches(request.getContrasena(), cliente.getContrasena())) {
            log.warn("Autenticación fallida: contraseña incorrecta para identificación [{}]", identificacion);
            throw new BadCredentialsException("Credenciales inválidas");
        }

        if (Boolean.FALSE.equals(cliente.getEstado())) {
            log.warn("Autenticación rechazada: el cliente con identificación [{}] se encuentra inactivo", identificacion);
            throw new BadCredentialsException("El cliente se encuentra inactivo");
        }

        log.debug("Generando claims y token JWT para cliente con ID [{}]", cliente.getId());
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", cliente.getId());
        claims.put("identificacion", cliente.getIdentificacion());
        claims.put("nombre", cliente.getNombre());
        claims.put("roles", List.of("ROLE_USER"));

        String token = jwtService.generateToken(String.valueOf(cliente.getId()), claims);
        log.info("Autenticación exitosa para cliente con ID [{}] - Token emitido con vigencia de {} ms",
                cliente.getId(), jwtService.getExpirationMs());

        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .id(cliente.getId())
                .expiresIn(jwtService.getExpirationMs())
                .build();
    }
}
