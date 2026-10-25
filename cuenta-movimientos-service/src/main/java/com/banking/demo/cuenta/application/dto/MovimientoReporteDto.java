package com.banking.demo.cuenta.application.dto;

import com.banking.demo.cuenta.domain.model.TipoMovimiento;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MovimientoReporteDto {

    @JsonProperty("fecha")
    @JsonAlias({"Fecha"})
    private String fecha;

    @JsonProperty("tipo")
    @JsonAlias({"Tipo", "tipoMovimiento"})
    private TipoMovimiento tipo;

    @JsonProperty("saldoInicial")
    @JsonAlias({"Saldo Inicial", "saldo_inicial", "SaldoInicial"})
    private BigDecimal saldoInicial;

    @JsonProperty("movimiento")
    @JsonAlias({"Movimiento", "valor"})
    private BigDecimal movimiento;

    @JsonProperty("saldoDisponible")
    @JsonAlias({"Saldo Disponible", "saldo_disponible", "SaldoDisponible", "saldo"})
    private BigDecimal saldoDisponible;

    @JsonProperty("estado")
    @JsonAlias({"Estado"})
    private Boolean estado;
}
