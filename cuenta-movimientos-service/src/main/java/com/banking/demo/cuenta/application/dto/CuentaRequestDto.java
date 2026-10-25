package com.banking.demo.cuenta.application.dto;

import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaRequestDto {

    @NotBlank(message = "El numero de cuenta es obligatorio")
    @Size(max = 30, message = "El numero de cuenta no debe exceder 30 caracteres")
    private String numeroCuenta;

    @NotNull(message = "El tipo de cuenta es obligatorio (AHORROS, CORRIENTE)")
    private TipoCuenta tipoCuenta;

    @NotNull(message = "El saldo inicial es obligatorio")
    @DecimalMin(value = "0.0", message = "El saldo inicial no puede ser negativo")
    private BigDecimal saldoInicial;

    private Boolean estado;

    @NotNull(message = "El identificador del cliente es obligatorio")
    @JsonAlias({"cliente", "id", "cliente_id"})
    private Long clienteId;
}
