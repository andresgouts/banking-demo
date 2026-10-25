package com.banking.demo.cuenta.application.service

import com.banking.demo.cuenta.application.service.impl.ClienteSnapshotServiceImpl
import com.banking.demo.cuenta.domain.model.ClienteSnapshot
import com.banking.demo.cuenta.domain.model.Cuenta
import com.banking.demo.cuenta.domain.model.TipoCuenta
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository
import com.banking.demo.cuenta.domain.repository.CuentaRepository
import com.banking.demo.event.ClientEventDto
import com.banking.demo.event.ClientEventType
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class ClienteSnapshotServiceSpec extends Specification {

    ClienteSnapshotRepository clienteSnapshotRepository = Mock()
    CuentaRepository cuentaRepository = Mock()

    @Subject
    ClienteSnapshotService clienteSnapshotService = new ClienteSnapshotServiceImpl(clienteSnapshotRepository, cuentaRepository)

    ClientEventDto sampleEvent

    def setup() {
        sampleEvent = ClientEventDto.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(true)
                .eventType(ClientEventType.CLIENT_CREATED.name())
                .timestamp(LocalDateTime.now())
                .build()
    }

    def "deberia sincronizar y crear un nuevo snapshot cuando no existe previamente"() {
        given: "El snapshot no existe previamente"
        clienteSnapshotRepository.findById(1L) >> Optional.empty()

        when: "Se sincroniza el evento de creacion de cliente"
        ClienteSnapshot result = clienteSnapshotService.sync(sampleEvent)

        then: "Se persiste el snapshot con los datos del evento"
        1 * clienteSnapshotRepository.save({ ClienteSnapshot s ->
            s.id == 1L && s.nombre == "Jose Lema"
        }) >> { ClienteSnapshot s -> s }
        result != null
        result.id == 1L
        result.nombre == "Jose Lema"
        result.identificacion == "098254785"
        result.estado == true
    }

    def "deberia actualizar un snapshot existente al recibir evento CLIENT_UPDATED"() {
        given: "Un snapshot existente en base de datos"
        def existing = ClienteSnapshot.builder()
                .id(1L)
                .nombre("Jose Lema Antiguo")
                .identificacion("098254785")
                .estado(true)
                .build()

        clienteSnapshotRepository.findById(1L) >> Optional.of(existing)

        def updateEvent = ClientEventDto.builder()
                .id(1L)
                .nombre("Jose Lema Actualizado")
                .identificacion("098254785")
                .estado(true)
                .eventType(ClientEventType.CLIENT_UPDATED.name())
                .build()

        when: "Se sincroniza el evento de actualizacion"
        ClienteSnapshot result = clienteSnapshotService.sync(updateEvent)

        then: "El snapshot existente se actualiza con el nuevo nombre"
        1 * clienteSnapshotRepository.save(existing) >> { ClienteSnapshot s -> s }
        result.nombre == "Jose Lema Actualizado"
        result.estado == true
    }

    def "deberia marcar estado como inactivo (false) y desactivar las cuentas al recibir evento CLIENT_DELETED"() {
        given: "Un snapshot activo existente y cuentas activas asociadas"
        def existing = ClienteSnapshot.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(true)
                .build()

        def cuenta1 = Cuenta.builder()
                .id(10L)
                .numeroCuenta("478758")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("2000.00"))
                .saldoActual(new BigDecimal("2000.00"))
                .estado(true)
                .clienteId(1L)
                .build()

        def cuenta2 = Cuenta.builder()
                .id(11L)
                .numeroCuenta("225487")
                .tipoCuenta(TipoCuenta.CORRIENTE)
                .saldoInicial(new BigDecimal("500.00"))
                .saldoActual(new BigDecimal("500.00"))
                .estado(false)
                .clienteId(1L)
                .build()

        clienteSnapshotRepository.findById(1L) >> Optional.of(existing)
        cuentaRepository.findByClienteId(1L) >> [cuenta1, cuenta2]

        def deleteEvent = ClientEventDto.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(false)
                .eventType(ClientEventType.CLIENT_DELETED.name())
                .build()

        when: "Se sincroniza el evento de eliminacion"
        ClienteSnapshot result = clienteSnapshotService.sync(deleteEvent)

        then: "El estado del snapshot pasa a inactivo (false) y las cuentas activas se desactivan"
        1 * clienteSnapshotRepository.save(existing) >> { ClienteSnapshot s -> s }
        1 * cuentaRepository.saveAll({ List<Cuenta> cuentas ->
            cuentas.size() == 1 && cuentas[0].numeroCuenta == "478758" && cuentas[0].estado == false
        })
        result.estado == false
        cuenta1.estado == false
    }

    def "deberia manejar CLIENT_DELETED cuando el cliente no tiene cuentas asociadas"() {
        given: "Un snapshot activo existente sin cuentas asociadas"
        def existing = ClienteSnapshot.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(true)
                .build()

        clienteSnapshotRepository.findById(1L) >> Optional.of(existing)
        cuentaRepository.findByClienteId(1L) >> []

        def deleteEvent = ClientEventDto.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(false)
                .eventType(ClientEventType.CLIENT_DELETED.name())
                .build()

        when: "Se sincroniza el evento de eliminacion"
        ClienteSnapshot result = clienteSnapshotService.sync(deleteEvent)

        then: "El estado del snapshot pasa a false y no se interactua con saveAll"
        1 * clienteSnapshotRepository.save(existing) >> { ClienteSnapshot s -> s }
        0 * cuentaRepository.saveAll(_)
        result.estado == false
    }

    def "deberia lanzar IllegalArgumentException si el id es nulo"() {
        given: "Un evento con id nulo"
        def invalidEvent = ClientEventDto.builder()
                .id(null)
                .nombre("Invalido")
                .eventType("CLIENT_CREATED")
                .build()

        when: "Se intenta sincronizar"
        clienteSnapshotService.sync(invalidEvent)

        then: "Se lanza IllegalArgumentException"
        def ex = thrown(IllegalArgumentException)
        ex.message.contains("El id del cliente no puede ser nulo")
    }
}
