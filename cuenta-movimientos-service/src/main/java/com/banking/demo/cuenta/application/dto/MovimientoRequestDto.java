package com.banking.demo.cuenta.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class MovimientoRequestDto {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Size(max = 30, message = "El numero de cuenta no debe exceder 30 caracteres")
    private String numeroCuenta;

    @NotNull(message = "El valor del movimiento es obligatorio")
    private BigDecimal valor;

    private LocalDateTime fecha;
}
