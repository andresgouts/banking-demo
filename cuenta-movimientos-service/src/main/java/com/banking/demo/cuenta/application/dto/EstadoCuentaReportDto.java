package com.banking.demo.cuenta.application.dto;

import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EstadoCuentaReportDto {

    @JsonProperty("numeroCuenta")
    @JsonAlias({"Numero Cuenta", "numero_cuenta"})
    private String numeroCuenta;

    @JsonProperty("tipo")
    @JsonAlias({"Tipo", "tipoCuenta", "tipo_cuenta"})
    private TipoCuenta tipo;

    @JsonProperty("saldo")
    @JsonAlias({"Saldo", "saldoActual", "saldo_actual"})
    private BigDecimal saldo;

    @JsonProperty("estado")
    @JsonAlias({"Estado"})
    private Boolean estado;

    @JsonProperty("movimientos")
    @JsonAlias({"Movimientos"})
    @Builder.Default
    private List<MovimientoReporteDto> movimientos = new ArrayList<>();
}
