package com.banking.demo.cliente.application.mapper;

import com.banking.demo.cliente.application.dto.ClientePatchDto;
import com.banking.demo.cliente.application.dto.ClienteRequestDto;
import com.banking.demo.cliente.application.dto.ClienteResponseDto;
import com.banking.demo.cliente.application.dto.ClienteUpdateDto;
import com.banking.demo.cliente.domain.model.Cliente;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class ClienteMapper {

    public Cliente toEntity(ClienteRequestDto dto, PasswordEncoder passwordEncoder) {
        if (dto == null) {
            return null;
        }

        return Cliente.builder()
                .nombre(dto.getNombre())
                .genero(dto.getGenero())
                .edad(dto.getEdad())
                .identificacion(dto.getIdentificacion())
                .direccion(dto.getDireccion())
                .telefono(dto.getTelefono())
                .contrasena(passwordEncoder.encode(dto.getContrasena()))
                .estado(dto.getEstado() != null ? dto.getEstado() : Boolean.TRUE)
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    public ClienteResponseDto toResponseDto(Cliente entity) {
        if (entity == null) {
            return null;
        }

        return ClienteResponseDto.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .genero(entity.getGenero())
                .edad(entity.getEdad())
                .identificacion(entity.getIdentificacion())
                .direccion(entity.getDireccion())
                .telefono(entity.getTelefono())
                .estado(entity.getEstado())
                .fechaCreacion(entity.getFechaCreacion())
                .fechaModificacion(entity.getFechaModificacion())
                .fechaEliminacion(entity.getFechaEliminacion())
                .build();
    }

    public void updateEntity(Cliente entity, ClienteUpdateDto dto, PasswordEncoder passwordEncoder) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setNombre(dto.getNombre());
        entity.setGenero(dto.getGenero());
        entity.setEdad(dto.getEdad());
        entity.setIdentificacion(dto.getIdentificacion());
        entity.setDireccion(dto.getDireccion());
        entity.setTelefono(dto.getTelefono());
        entity.setEstado(dto.getEstado());
        entity.setFechaModificacion(LocalDateTime.now());

        if (dto.getContrasena() != null && !dto.getContrasena().isBlank()) {
            entity.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        }
    }

    public void patchEntity(Cliente entity, ClientePatchDto dto, PasswordEncoder passwordEncoder) {
        if (entity == null || dto == null) {
            return;
        }

        if (dto.getNombre() != null && !dto.getNombre().isBlank()) {
            entity.setNombre(dto.getNombre());
        }
        if (dto.getGenero() != null) {
            entity.setGenero(dto.getGenero());
        }
        if (dto.getEdad() != null) {
            entity.setEdad(dto.getEdad());
        }
        if (dto.getDireccion() != null) {
            entity.setDireccion(dto.getDireccion());
        }
        if (dto.getTelefono() != null) {
            entity.setTelefono(dto.getTelefono());
        }
        if (dto.getEstado() != null) {
            entity.setEstado(dto.getEstado());
        }
        if (dto.getContrasena() != null && !dto.getContrasena().isBlank()) {
            entity.setContrasena(passwordEncoder.encode(dto.getContrasena()));
        }
        entity.setFechaModificacion(LocalDateTime.now());
    }
}
