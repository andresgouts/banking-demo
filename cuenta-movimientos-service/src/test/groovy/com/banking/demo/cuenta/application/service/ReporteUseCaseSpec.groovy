package com.banking.demo.cuenta.application.service

import com.banking.demo.cuenta.application.dto.EstadoCuentaReportDto
import com.banking.demo.cuenta.application.service.impl.ReporteServiceImpl
import com.banking.demo.cuenta.domain.model.Cuenta
import com.banking.demo.cuenta.domain.model.Movimiento
import com.banking.demo.cuenta.domain.model.TipoCuenta
import com.banking.demo.cuenta.domain.model.TipoMovimiento
import com.banking.demo.cuenta.domain.repository.CuentaRepository
import com.banking.demo.cuenta.domain.repository.MovimientoRepository
import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import java.time.LocalDateTime

class ReporteUseCaseSpec extends Specification {

    MovimientoRepository movimientoRepository = Mock()
    CuentaRepository cuentaRepository = Mock()

    @Subject
    ReporteService reporteService = new ReporteServiceImpl(
            movimientoRepository,
            cuentaRepository
    )

    def "deberia generar reporte de estado de cuenta agrupado por cuenta con movimientos para un clienteId"() {
        given: "Una cuenta asociada al cliente 2L y un movimiento registrado"
        def cuenta = Cuenta.builder()
                .numeroCuenta("225487")
                .tipoCuenta(TipoCuenta.CORRIENTE)
                .saldoInicial(BigDecimal.valueOf(100.0))
                .saldoActual(BigDecimal.valueOf(700.0))
                .estado(true)
                .clienteId(2L)
                .build()

        def movEntity = Movimiento.builder()
                .id(1L)
                .cuenta(cuenta)
                .numeroCuenta("225487")
                .fecha(LocalDateTime.of(2022, 2, 10, 15, 30))
                .tipoMovimiento(TipoMovimiento.DEPOSITO)
                .valor(BigDecimal.valueOf(600.0))
                .saldo(BigDecimal.valueOf(700.0))
                .build()

        movimientoRepository.findByClienteIdAndFechaBetween(2L, _, _) >> [movEntity]

        when: "Se genera el reporte con fechas en formato dd/MM/yyyy y clienteId"
        List<EstadoCuentaReportDto> reporte = reporteService.generarReporte("10/02/2022", "10/02/2022", 2L)

        then: "El reporte contiene la cuenta agrupada con su saldo y lista de movimientos"
        reporte != null
        reporte.size() == 1

        def primerItem = reporte[0]
        primerItem.numeroCuenta == "225487"
        primerItem.tipo == TipoCuenta.CORRIENTE
        primerItem.saldo == BigDecimal.valueOf(700.0)
        primerItem.estado == true
        primerItem.movimientos.size() == 1

        def primerMov = primerItem.movimientos[0]
        primerMov.fecha == "10/02/2022"
        primerMov.tipo == TipoMovimiento.DEPOSITO
        primerMov.saldoInicial == BigDecimal.valueOf(100.0)
        primerMov.movimiento == BigDecimal.valueOf(600.0)
        primerMov.saldoDisponible == BigDecimal.valueOf(700.0)
        primerMov.estado == true
    }

    def "deberia filtrar por rango de fechas pasando fechaInicio y fechaFin correctas al repositorio"() {
        when: "Se invoca con rango '01/02/2022' y '15/02/2022'"
        reporteService.generarReporte("01/02/2022", "15/02/2022", 1L)

        then: "Se consulta el repositorio con start 2022-02-01T00:00:00 y end 2022-02-15T23:59:59.999999999"
        1 * movimientoRepository.findByClienteIdAndFechaBetween(
                1L,
                LocalDateTime.of(2022, 2, 1, 0, 0, 0),
                LocalDateTime.of(2022, 2, 15, 23, 59, 59, 999_999_999)
        ) >> Collections.emptyList()
    }

    def "deberia retornar lista vacia cuando el clienteId no tiene movimientos"() {
        given: "Un clienteId sin movimientos registrados"
        movimientoRepository.findByClienteIdAndFechaBetween(999L, _, _) >> Collections.emptyList()

        when: "Se intenta generar reporte"
        def reporte = reporteService.generarReporte("10/02/2022", "10/02/2022", 999L)

        then: "Retorna lista vacia"
        reporte != null
        reporte.isEmpty()
    }

    def "deberia lanzar IllegalArgumentException cuando la fecha inicio es posterior a fecha fin"() {
        when: "Rango invertido"
        reporteService.generarReporte("15/02/2022", "01/02/2022", 2L)

        then: "Lanza IllegalArgumentException"
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("no puede ser posterior a la fecha de fin")
    }

    @Unroll
    def "deberia rechazar peticion con parametros invalidos: '#descripcion'"() {
        when: "Se invocan parametros no permitidos"
        reporteService.generarReporte(fechaInicio, fechaFin, (Long) clienteId)

        then: "Lanza IllegalArgumentException"
        thrown(IllegalArgumentException)

        where:
        descripcion           | fechaInicio  | fechaFin     | clienteId
        "fechaInicio nula"    | null         | "15/02/2022" | 2L
        "fechaInicio vacia"   | "   "        | "15/02/2022" | 2L
        "fechaFin nula"       | "01/02/2022" | null         | 2L
        "fechaFin vacia"      | "01/02/2022" | "   "        | 2L
        "clienteId nulo"      | "01/02/2022" | "15/02/2022" | null
        "formato corrupto"    | "not-a-date" | "15/02/2022" | 2L
        "formato yyyy-MM-dd"  | "2022-02-01" | "2022-02-15" | 2L
    }

    def "deberia recuperar cuenta desde repositorio cuando movimiento no tiene cuenta embebida"() {
        given: "Un cliente y un movimiento con referencia a numeroCuenta pero cuenta null"
        def cuenta = Cuenta.builder()
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .estado(true)
                .saldoInicial(BigDecimal.valueOf(775.0))
                .saldoActual(BigDecimal.valueOf(200.0))
                .clienteId(2L)
                .build()

        def movEntity = Movimiento.builder()
                .id(99L)
                .cuenta(null)
                .numeroCuenta("478758")
                .fecha(LocalDateTime.of(2022, 2, 8, 10, 0))
                .valor(BigDecimal.valueOf(-575.0))
                .saldo(BigDecimal.valueOf(200.0))
                .build()

        movimientoRepository.findByClienteIdAndFechaBetween(2L, _, _) >> [movEntity]
        cuentaRepository.findByNumeroCuenta("478758") >> Optional.of(cuenta)

        when: "Se genera el reporte"
        def reporte = reporteService.generarReporte("08/02/2022", "08/02/2022", 2L)

        then: "La cuenta se resuelve desde cuentaRepository"
        reporte.size() == 1
        reporte[0].numeroCuenta == "478758"
        reporte[0].tipo == TipoCuenta.AHORROS
        reporte[0].saldo == BigDecimal.valueOf(200.0)
        reporte[0].movimientos.size() == 1
        reporte[0].movimientos[0].saldoDisponible == BigDecimal.valueOf(200.0)
        reporte[0].movimientos[0].saldoInicial == BigDecimal.valueOf(775.0)
    }
}
