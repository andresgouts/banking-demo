package com.banking.demo.cuenta.infrastructure.adapter.in.rest;

import com.banking.demo.cuenta.application.dto.EstadoCuentaReportDto;
import com.banking.demo.cuenta.application.service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Slf4j
@RestController
@RequestMapping({"/api/reportes", "/reportes"})
@RequiredArgsConstructor
public class ReporteController {

    private final ReporteService reporteService;

    @GetMapping
    public ResponseEntity<List<EstadoCuentaReportDto>> getReporteEstadoCuenta(
            @RequestParam(name = "fechaInicio", required = false) String fechaInicio,
            @RequestParam(name = "fechaFin", required = false) String fechaFin,
            @RequestParam(name = "clienteId", required = false) Long clienteId) {

        if (clienteId == null) {
            log.warn("Peticion GET a /reportes con parametro clienteId vacio");
            throw new IllegalArgumentException("El parametro 'clienteId' es obligatorio");
        }

        if (fechaInicio == null || fechaInicio.isBlank() || fechaFin == null || fechaFin.isBlank()) {
            log.warn("Peticion GET a /reportes sin parametros obligatorios fechaInicio o fechaFin");
            throw new IllegalArgumentException("Los parametros 'fechaInicio' y 'fechaFin' son obligatorios (ej. fechaInicio=01/02/2022&fechaFin=15/02/2022)");
        }

        log.info("Recibida peticion GET a /reportes con rango [{}] - [{}] y clienteId [{}]",
                fechaInicio, fechaFin, clienteId);
        List<EstadoCuentaReportDto> reporte = reporteService.generarReporte(fechaInicio, fechaFin, clienteId);

        log.info("Peticion GET /reportes completada con exito. Se retornan {} cuentas para el cliente [{}]",
                reporte.size(), clienteId);
        return ResponseEntity.ok(reporte);
    }
}
