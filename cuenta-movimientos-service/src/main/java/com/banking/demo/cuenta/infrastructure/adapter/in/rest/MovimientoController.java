package com.banking.demo.cuenta.infrastructure.adapter.in.rest;

import com.banking.demo.cuenta.application.dto.MovimientoRequestDto;
import com.banking.demo.cuenta.application.dto.MovimientoResponseDto;
import com.banking.demo.cuenta.application.service.MovimientoService;
import com.banking.demo.security.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/movimientos", "/movimientos"})
@RequiredArgsConstructor
public class MovimientoController {

    private final MovimientoService movimientoService;

    @PostMapping
    public ResponseEntity<MovimientoResponseDto> create(@Valid @RequestBody MovimientoRequestDto dto) {
        log.info("Recibida peticion POST para registrar movimiento inmutable en cuenta [{}] por valor [{}]",
                dto.getNumeroCuenta(), dto.getValor());
        MovimientoResponseDto created = movimientoService.registrarMovimiento(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        log.info("Movimiento registrado con exito con ID [{}] para cuenta [{}]",
                created.getId(), created.getNumeroCuenta());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<MovimientoResponseDto>> getAll(
            @RequestParam(required = false) String numeroCuenta,
            Authentication authentication) {
        if (numeroCuenta != null && !numeroCuenta.isBlank()) {
            log.info("Recibida peticion GET para listar movimientos de la cuenta [{}]", numeroCuenta);
            List<MovimientoResponseDto> list = movimientoService.findByNumeroCuenta(numeroCuenta);
            log.info("Se retornan {} movimientos para la cuenta [{}]", list.size(), numeroCuenta);
            return ResponseEntity.ok(list);
        }

        Long clienteId = null;
        if (authentication != null && authentication.getName() != null && !authentication.getName().isBlank()) {
            try {
                clienteId = Long.parseLong(authentication.getName().trim());
            } catch (NumberFormatException ignored) {
            }
        }
        if (clienteId == null) {
            clienteId = SecurityUtils.getAuthenticatedId();
        }

        log.info("Recibida peticion GET para listar movimientos del cliente autenticado [{}]", clienteId);
        List<MovimientoResponseDto> list = movimientoService.findByClienteId(clienteId);
        log.info("Se retornan {} movimientos para el cliente [{}]", list.size(), clienteId);
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MovimientoResponseDto> getById(@PathVariable Long id) {
        log.info("Recibida peticion GET para consultar movimiento ID [{}]", id);
        MovimientoResponseDto dto = movimientoService.findById(id);
        log.info("Movimiento ID [{}] recuperado exitosamente", id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    public ResponseEntity<List<MovimientoResponseDto>> getByNumeroCuenta(@PathVariable String numeroCuenta) {
        log.info("Recibida peticion GET para consultar movimientos de cuenta [{}]", numeroCuenta);
        List<MovimientoResponseDto> list = movimientoService.findByNumeroCuenta(numeroCuenta);
        log.info("Se retornan {} movimientos para la cuenta [{}]", list.size(), numeroCuenta);
        return ResponseEntity.ok(list);
    }
}
