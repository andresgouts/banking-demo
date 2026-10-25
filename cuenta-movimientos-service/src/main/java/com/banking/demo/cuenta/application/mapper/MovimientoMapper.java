package com.banking.demo.cuenta.application.mapper;

import com.banking.demo.cuenta.application.dto.MovimientoRequestDto;
import com.banking.demo.cuenta.application.dto.MovimientoResponseDto;
import com.banking.demo.cuenta.domain.model.Cuenta;
import com.banking.demo.cuenta.domain.model.Movimiento;
import com.banking.demo.cuenta.domain.model.TipoMovimiento;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Component
public class MovimientoMapper {

    public Movimiento toEntity(MovimientoRequestDto dto, Cuenta cuenta, TipoMovimiento tipoMovimiento, BigDecimal nuevoSaldo) {
        if (dto == null) {
            return null;
        }

        LocalDateTime fecha = dto.getFecha() != null ? dto.getFecha() : LocalDateTime.now();

        return Movimiento.builder()
                .cuenta(cuenta)
                .numeroCuenta(cuenta != null ? cuenta.getNumeroCuenta() : dto.getNumeroCuenta())
                .fecha(fecha)
                .tipoMovimiento(tipoMovimiento)
                .valor(dto.getValor())
                .saldo(nuevoSaldo)
                .build();
    }

    public MovimientoResponseDto toResponseDto(Movimiento entity, String nombreCliente) {
        if (entity == null) {
            return null;
        }

        Cuenta cuenta = entity.getCuenta();
        BigDecimal saldoInicial = (entity.getSaldo() != null && entity.getValor() != null)
                ? entity.getSaldo().subtract(entity.getValor())
                : null;

        return MovimientoResponseDto.builder()
                .id(entity.getId())
                .numeroCuenta(entity.getNumeroCuenta())
                .tipoCuenta(cuenta != null ? cuenta.getTipoCuenta() : null)
                .fecha(entity.getFecha())
                .tipoMovimiento(entity.getTipoMovimiento())
                .saldoInicial(saldoInicial)
                .valor(entity.getValor())
                .saldo(entity.getSaldo())
                .estadoCuenta(cuenta != null ? cuenta.getEstado() : null)
                .clienteId(cuenta != null ? cuenta.getClienteId() : null)
                .nombreCliente(nombreCliente)
                .build();
    }
}
