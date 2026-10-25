package com.banking.demo.cuenta.domain.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum TipoCuenta {
    AHORROS,
    CORRIENTE;

    @JsonCreator
    public static TipoCuenta fromString(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        for (TipoCuenta tc : TipoCuenta.values()) {
            if (tc.name().equalsIgnoreCase(value.trim())) {
                return tc;
            }
        }
        throw new IllegalArgumentException("Tipo de cuenta no valido: " + value + ". Valores permitidos: AHORROS, CORRIENTE");
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
