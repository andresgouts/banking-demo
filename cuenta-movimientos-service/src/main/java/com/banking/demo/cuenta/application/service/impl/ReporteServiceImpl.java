package com.banking.demo.cuenta.application.service.impl;

import com.banking.demo.cuenta.application.dto.EstadoCuentaReportDto;
import com.banking.demo.cuenta.application.dto.MovimientoReporteDto;
import com.banking.demo.cuenta.application.service.ReporteService;
import com.banking.demo.cuenta.domain.model.Cuenta;
import com.banking.demo.cuenta.domain.model.Movimiento;
import com.banking.demo.cuenta.domain.model.TipoCuenta;
import com.banking.demo.cuenta.domain.repository.CuentaRepository;
import com.banking.demo.cuenta.domain.repository.MovimientoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReporteServiceImpl implements ReporteService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MovimientoRepository movimientoRepository;
    private final CuentaRepository cuentaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<EstadoCuentaReportDto> generarReporte(String fechaInicio, String fechaFin, Long clienteId) {
        log.info("Generando reporte de estado de cuenta para cliente con ID [{}] entre [{}] y [{}]", clienteId, fechaInicio, fechaFin);

        if (clienteId == null) {
            log.warn("Rechazo de reporte: parametro clienteId nulo");
            throw new IllegalArgumentException("El parametro 'clienteId' es obligatorio");
        }

        DateTimeRange range = validarRangoFechas(fechaInicio, fechaFin);
        return ejecutarReporte(range, clienteId);
    }

    private DateTimeRange validarRangoFechas(String fechaInicio, String fechaFin) {
        if (fechaInicio == null || fechaInicio.isBlank() || fechaFin == null || fechaFin.isBlank()) {
            log.warn("Rechazo de reporte: fechaInicio o fechaFin no proporcionados");
            throw new IllegalArgumentException("Los parametros 'fechaInicio' y 'fechaFin' son obligatorios (ej. fechaInicio=01/02/2022&fechaFin=15/02/2022)");
        }

        LocalDate startDate = parseDate(fechaInicio);
        LocalDate endDate = parseDate(fechaFin);

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59, 999_999_999);

        if (startDateTime.isAfter(endDateTime)) {
            log.warn("Rango de fechas invalido: fechaInicio [{}] posterior a fechaFin [{}]", fechaInicio, fechaFin);
            throw new IllegalArgumentException("La fecha de inicio [" + fechaInicio + "] no puede ser posterior a la fecha de fin [" + fechaFin + "]");
        }

        return new DateTimeRange(startDateTime, endDateTime);
    }

    private List<EstadoCuentaReportDto> ejecutarReporte(DateTimeRange range, Long clienteId) {
        log.debug("Recuperando movimientos para reporte con cliente ID [{}]", clienteId);

        List<Movimiento> movimientos = movimientoRepository.findByClienteIdAndFechaBetween(
                clienteId,
                range.start(),
                range.end()
        );

        log.info("Se recuperaron {} movimientos para cliente con ID [{}] entre {} y {}",
                movimientos.size(), clienteId, range.start(), range.end());

        if (movimientos.isEmpty()) {
            return List.of();
        }

        Map<String, List<Movimiento>> movimientosPorCuenta = movimientos.stream()
                .collect(Collectors.groupingBy(
                        Movimiento::getNumeroCuenta,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<EstadoCuentaReportDto> reporte = new ArrayList<>();

        for (Map.Entry<String, List<Movimiento>> entry : movimientosPorCuenta.entrySet()) {
            String numeroCuenta = entry.getKey();
            List<Movimiento> movs = entry.getValue();

            Cuenta cuenta = movs.get(0).getCuenta();
            if (cuenta == null && numeroCuenta != null) {
                cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta).orElse(null);
            }

            TipoCuenta tipo = cuenta != null ? cuenta.getTipoCuenta() : null;
            Boolean estado = cuenta != null ? cuenta.getEstado() : Boolean.TRUE;
            BigDecimal saldo = (cuenta != null && cuenta.getSaldoActual() != null)
                    ? cuenta.getSaldoActual()
                    : (cuenta != null ? cuenta.getSaldoInicial() : (movs.isEmpty() ? BigDecimal.ZERO : movs.get(0).getSaldo()));

            List<MovimientoReporteDto> movsDto = movs.stream()
                    .map(this::mapToMovimientoReportDto)
                    .toList();

            reporte.add(EstadoCuentaReportDto.builder()
                    .numeroCuenta(numeroCuenta)
                    .tipo(tipo)
                    .saldo(saldo)
                    .estado(estado)
                    .movimientos(movsDto)
                    .build());
        }

        return reporte;
    }

    private MovimientoReporteDto mapToMovimientoReportDto(Movimiento movimiento) {
        BigDecimal valorMovimiento = movimiento.getValor();
        BigDecimal saldoDisponible = movimiento.getSaldo();
        BigDecimal saldoInicial = (saldoDisponible != null && valorMovimiento != null)
                ? saldoDisponible.subtract(valorMovimiento)
                : BigDecimal.ZERO;

        String fechaFormatted = movimiento.getFecha() != null
                ? movimiento.getFecha().format(DATE_FORMATTER)
                : "";

        return MovimientoReporteDto.builder()
                .fecha(fechaFormatted)
                .tipo(movimiento.getTipoMovimiento())
                .saldoInicial(saldoInicial)
                .movimiento(valorMovimiento)
                .saldoDisponible(saldoDisponible)
                .estado(movimiento.getCuenta() != null ? movimiento.getCuenta().getEstado() : Boolean.TRUE)
                .build();
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("Fecha no valida: valor vacio");
        }
        try {
            return LocalDate.parse(dateStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Formato de fecha no valido: " + dateStr +
                    ". Formato soportado: dd/MM/yyyy");
        }
    }

    private record DateTimeRange(LocalDateTime start, LocalDateTime end) {}
}
