package com.banking.demo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientEventDto implements Serializable {

    private Long id;
    private String nombre;
    private String identificacion;
    private Boolean estado;
    private String eventType;

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
