package com.banking.demo.cuenta.application.dto;

import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CuentaPatchDto {

    private TipoCuenta tipoCuenta;

    private Boolean estado;

    @JsonAlias({"cliente", "id", "cliente_id"})
    private Long clienteId;
}
