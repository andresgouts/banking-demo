package com.banking.demo.cliente.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClienteUpdateDto {

    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;

    private String genero;

    @Min(value = 0, message = "La edad no puede ser negativa")
    private Integer edad;

    @NotBlank(message = "La identificación es obligatoria")
    private String identificacion;

    private String direccion;

    private String telefono;

    private String contrasena;

    @NotNull(message = "El estado es obligatorio")
    private Boolean estado;
}
