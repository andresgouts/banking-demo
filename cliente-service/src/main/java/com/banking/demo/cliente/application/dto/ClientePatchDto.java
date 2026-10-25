package com.banking.demo.cliente.application.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientePatchDto {

    private String nombre;
    private String genero;

    @Min(value = 0, message = "La edad no puede ser negativa")
    private Integer edad;

    private String direccion;
    private String telefono;
    private String contrasena;
    private Boolean estado;
}
