package com.banking.demo.cuenta.application.service;

import com.banking.demo.cuenta.application.dto.EstadoCuentaReportDto;

import java.util.List;

public interface ReporteService {

    List<EstadoCuentaReportDto> generarReporte(String fechaInicio, String fechaFin, Long clienteId);
}
