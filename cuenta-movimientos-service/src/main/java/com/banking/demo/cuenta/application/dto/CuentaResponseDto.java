package com.banking.demo.cuenta.application.dto;

import com.banking.demo.cuenta.domain.model.TipoCuenta;
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
public class CuentaResponseDto {

    private Long id;
    private String numeroCuenta;
    private TipoCuenta tipoCuenta;
    private BigDecimal saldoInicial;
    private BigDecimal saldoActual;
    private Boolean estado;
    private Long clienteId;
    private String nombreCliente;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaModificacion;
}
