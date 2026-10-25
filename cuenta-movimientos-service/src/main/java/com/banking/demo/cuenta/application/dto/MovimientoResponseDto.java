package com.banking.demo.cuenta.application.dto;

import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.banking.demo.cuenta.domain.model.TipoMovimiento;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoResponseDto {

    private Long id;
    private String numeroCuenta;
    private TipoCuenta tipoCuenta;
    private LocalDateTime fecha;
    private TipoMovimiento tipoMovimiento;
    private BigDecimal saldoInicial;
    private BigDecimal valor;
    private BigDecimal saldo;
    private Boolean estadoCuenta;
    private Long clienteId;
    private String nombreCliente;
}
