package com.banking.demo.cuenta.application.mapper;

import com.banking.demo.cuenta.application.dto.CuentaPatchDto;
import com.banking.demo.cuenta.application.dto.CuentaRequestDto;
import com.banking.demo.cuenta.application.dto.CuentaResponseDto;
import com.banking.demo.cuenta.application.dto.CuentaUpdateDto;
import com.banking.demo.cuenta.domain.model.Cuenta;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CuentaMapper {

    public Cuenta toEntity(CuentaRequestDto dto, Long resolvedClienteId) {
        if (dto == null) {
            return null;
        }

        return Cuenta.builder()
                .numeroCuenta(dto.getNumeroCuenta().trim())
                .tipoCuenta(dto.getTipoCuenta())
                .saldoInicial(dto.getSaldoInicial())
                .saldoActual(dto.getSaldoInicial())
                .estado(dto.getEstado() != null ? dto.getEstado() : Boolean.TRUE)
                .clienteId(resolvedClienteId != null ? resolvedClienteId : dto.getClienteId())
                .fechaCreacion(LocalDateTime.now())
                .build();
    }

    public CuentaResponseDto toResponseDto(Cuenta entity, String nombreCliente) {
        if (entity == null) {
            return null;
        }

        return CuentaResponseDto.builder()
                .id(entity.getId())
                .numeroCuenta(entity.getNumeroCuenta())
                .tipoCuenta(entity.getTipoCuenta())
                .saldoInicial(entity.getSaldoInicial())
                .saldoActual(entity.getSaldoActual())
                .estado(entity.getEstado())
                .clienteId(entity.getClienteId())
                .nombreCliente(nombreCliente)
                .fechaCreacion(entity.getFechaCreacion())
                .fechaModificacion(entity.getFechaModificacion())
                .build();
    }

    public void updateEntity(Cuenta entity, CuentaUpdateDto dto, Long resolvedClienteId) {
        if (entity == null || dto == null) {
            return;
        }

        entity.setTipoCuenta(dto.getTipoCuenta());
        entity.setEstado(dto.getEstado());
        if (resolvedClienteId != null) {
            entity.setClienteId(resolvedClienteId);
        }
        entity.setFechaModificacion(LocalDateTime.now());
    }

    public void patchEntity(Cuenta entity, CuentaPatchDto dto, Long resolvedClienteId) {
        if (entity == null || dto == null) {
            return;
        }

        if (dto.getTipoCuenta() != null) {
            entity.setTipoCuenta(dto.getTipoCuenta());
        }
        if (dto.getEstado() != null) {
            entity.setEstado(dto.getEstado());
        }
        if (resolvedClienteId != null) {
            entity.setClienteId(resolvedClienteId);
        }
        entity.setFechaModificacion(LocalDateTime.now());
    }
}
