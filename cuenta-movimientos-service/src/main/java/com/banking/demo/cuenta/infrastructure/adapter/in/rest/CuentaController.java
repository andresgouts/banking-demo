package com.banking.demo.cuenta.infrastructure.adapter.in.rest;

import com.banking.demo.cuenta.application.dto.CuentaPatchDto;
import com.banking.demo.cuenta.application.dto.CuentaRequestDto;
import com.banking.demo.cuenta.application.dto.CuentaResponseDto;
import com.banking.demo.cuenta.application.dto.CuentaUpdateDto;
import com.banking.demo.cuenta.application.service.CuentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/cuentas", "/cuentas"})
@RequiredArgsConstructor
public class CuentaController {

    private final CuentaService cuentaService;

    @PostMapping
    public ResponseEntity<CuentaResponseDto> create(@Valid @RequestBody CuentaRequestDto dto) {
        log.info("Recibida peticion POST para crear cuenta numero: [{}]", dto.getNumeroCuenta());
        CuentaResponseDto created = cuentaService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{numeroCuenta}")
                .buildAndExpand(created.getNumeroCuenta())
                .toUri();
        log.info("Peticion POST completada con exito. Cuenta creada: [{}]", created.getNumeroCuenta());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<CuentaResponseDto>> getAll(Authentication authentication) {
        Long clienteId = null;
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            try {
                clienteId = Long.parseLong(authentication.getName().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        log.info("Recibida peticion GET para consultar cuentas del usuario autenticado [{}]", clienteId);
        List<CuentaResponseDto> list = cuentaService.findByClienteId(clienteId);
        log.info("Peticion GET completada. Se retornan {} cuentas para el cliente [{}]", list.size(), clienteId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDto> getByNumeroCuenta(@PathVariable String numeroCuenta) {
        log.info("Recibida peticion GET para consultar cuenta numero: [{}]", numeroCuenta);
        CuentaResponseDto response = cuentaService.findByNumeroCuenta(numeroCuenta);
        log.info("Peticion GET para cuenta [{}] completada con exito", numeroCuenta);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDto> update(@PathVariable String numeroCuenta,
                                                     @Valid @RequestBody CuentaUpdateDto dto) {
        log.info("Recibida peticion PUT para actualizar cuenta numero: [{}]", numeroCuenta);
        CuentaResponseDto updated = cuentaService.update(numeroCuenta, dto);
        log.info("Peticion PUT para cuenta [{}] completada con exito", numeroCuenta);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponseDto> patch(@PathVariable String numeroCuenta,
                                                    @RequestBody CuentaPatchDto dto) {
        log.info("Recibida peticion PATCH para actualizar cuenta numero: [{}]", numeroCuenta);
        CuentaResponseDto patched = cuentaService.patch(numeroCuenta, dto);
        log.info("Peticion PATCH para cuenta [{}] completada con exito", numeroCuenta);
        return ResponseEntity.ok(patched);
    }
}
