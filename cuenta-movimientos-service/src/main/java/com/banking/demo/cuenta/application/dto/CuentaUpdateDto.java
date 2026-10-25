package com.banking.demo.cuenta.application.dto;

import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaUpdateDto {

    @NotNull(message = "El tipo de cuenta es obligatorio (AHORROS, CORRIENTE)")
    private TipoCuenta tipoCuenta;

    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;

    @JsonAlias({"cliente", "id", "cliente_id"})
    private Long clienteId;
}
