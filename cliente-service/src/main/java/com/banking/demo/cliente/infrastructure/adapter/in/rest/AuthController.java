package com.banking.demo.cliente.infrastructure.adapter.in.rest;

import com.banking.demo.cliente.application.service.AuthService;
import com.banking.demo.security.dto.AuthRequest;
import com.banking.demo.security.dto.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest request) {
        String identificacion = request != null ? request.getIdentificacion() : "null";
        log.info("Recibida solicitud POST /api/auth/login para identificacion: {}", identificacion);
        AuthResponse response = authService.login(request);
        log.info("Solicitud POST /api/auth/login completada exitosamente para cliente con ID: {}", response.getId());
        return ResponseEntity.ok(response);
    }
}
