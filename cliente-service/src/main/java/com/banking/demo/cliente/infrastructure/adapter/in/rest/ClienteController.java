package com.banking.demo.cliente.infrastructure.adapter.in.rest;

import com.banking.demo.cliente.application.dto.ClientePatchDto;
import com.banking.demo.cliente.application.dto.ClienteRequestDto;
import com.banking.demo.cliente.application.dto.ClienteResponseDto;
import com.banking.demo.cliente.application.dto.ClienteUpdateDto;
import com.banking.demo.cliente.application.service.ClienteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping({"/api/clientes", "/clientes"})
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @PostMapping
    public ResponseEntity<ClienteResponseDto> create(@Valid @RequestBody ClienteRequestDto dto) {
        log.info("Recibida petición POST para registrar cliente con identificación: [{}]", dto.getIdentificacion());
        ClienteResponseDto created = clienteService.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        log.info("Petición POST completada con éxito. Cliente creado con ID: [{}]", created.getId());
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponseDto>> getAll() {
        log.info("Recibida petición GET para consultar todos los clientes");
        List<ClienteResponseDto> list = clienteService.findAll();
        log.info("Petición GET completada. Se retornan {} clientes", list.size());
        return ResponseEntity.ok(list);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> getById(@PathVariable Long id) {
        log.info("Recibida petición GET para consultar cliente con ID: [{}]", id);
        ClienteResponseDto response = clienteService.findById(id);
        log.info("Petición GET para cliente [{}] completada con éxito", id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> update(@PathVariable Long id,
                                                     @Valid @RequestBody ClienteUpdateDto dto) {
        log.info("Recibida petición PUT para actualizar cliente con ID: [{}]", id);
        ClienteResponseDto updated = clienteService.update(id, dto);
        log.info("Petición PUT para cliente [{}] completada con éxito", id);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ClienteResponseDto> patch(@PathVariable Long id,
                                                    @RequestBody ClientePatchDto dto) {
        log.info("Recibida petición PATCH para actualizar cliente con ID: [{}]", id);
        ClienteResponseDto patched = clienteService.patch(id, dto);
        log.info("Petición PATCH para cliente [{}] completada con éxito", id);
        return ResponseEntity.ok(patched);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Recibida petición DELETE para eliminar cliente con ID: [{}]", id);
        clienteService.delete(id);
        log.info("Petición DELETE para cliente [{}] completada con éxito", id);
        return ResponseEntity.noContent().build();
    }
}
