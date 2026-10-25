package com.banking.demo.cuenta.infrastructure.adapter.in.rest

import com.banking.demo.cuenta.application.dto.EstadoCuentaReportDto
import com.banking.demo.cuenta.application.service.ReporteService
import org.springframework.http.HttpStatus
import spock.lang.Specification
import spock.lang.Subject

class ReporteControllerSpec extends Specification {

    ReporteService reporteService = Mock()

    @Subject
    ReporteController controller = new ReporteController(reporteService)

    def "deberia invocar reporteService.generarReporte con clienteId de tipo Long y retornar 200 OK"() {
        given: "Parametros validos"
        String fechaInicio = "01/02/2022"
        String fechaFin = "15/02/2022"
        Long clienteId = 1L
        def listaReporte = [EstadoCuentaReportDto.builder().numeroCuenta("478758").build()]

        when: "Se solicita el reporte con clienteId Long"
        def response = controller.getReporteEstadoCuenta(fechaInicio, fechaFin, clienteId)

        then: "Retorna 200 OK y la lista del reporte"
        1 * reporteService.generarReporte(fechaInicio, fechaFin, clienteId) >> listaReporte
        response.statusCode == HttpStatus.OK
        response.body != null
        response.body.size() == 1
        response.body[0].numeroCuenta == "478758"
    }

    def "deberia lanzar IllegalArgumentException cuando clienteId es nulo"() {
        when: "Se solicita el reporte sin clienteId"
        controller.getReporteEstadoCuenta("01/02/2022", "15/02/2022", null)

        then: "Lanza IllegalArgumentException"
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("El parametro 'clienteId' es obligatorio")
        0 * reporteService.generarReporte(_, _, _)
    }

    def "deberia lanzar IllegalArgumentException cuando fechaInicio o fechaFin son vacios"() {
        when: "Se solicita el reporte sin fechaInicio"
        controller.getReporteEstadoCuenta("", "15/02/2022", 1L)

        then: "Lanza IllegalArgumentException"
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("Los parametros 'fechaInicio' y 'fechaFin' son obligatorios")
        0 * reporteService.generarReporte(_, _, _)
    }
}
