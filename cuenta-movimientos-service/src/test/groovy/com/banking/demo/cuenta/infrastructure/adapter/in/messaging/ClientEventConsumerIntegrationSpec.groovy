package com.banking.demo.cuenta.infrastructure.adapter.in.messaging

import com.banking.demo.cuenta.domain.model.ClienteSnapshot
import com.banking.demo.cuenta.domain.model.Cuenta
import com.banking.demo.cuenta.domain.model.TipoCuenta
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository
import com.banking.demo.cuenta.domain.repository.CuentaRepository
import com.banking.demo.event.ClientEventDto
import com.banking.demo.event.ClientEventType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Transactional
import spock.lang.Specification

import java.math.BigDecimal
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles("local")
@Transactional
class ClientEventConsumerIntegrationSpec extends Specification {

    @Autowired
    ClientEventConsumer clientEventConsumer

    @Autowired
    ClienteSnapshotRepository clienteSnapshotRepository

    @Autowired
    CuentaRepository cuentaRepository

    def setup() {
        cuentaRepository.deleteAll()
        clienteSnapshotRepository.deleteAll()
    }

    def "deberia persistir ClienteSnapshot al consumir evento CLIENT_CREATED"() {
        given: "Un evento de creacion de cliente"
        def event = ClientEventDto.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(true)
                .eventType(ClientEventType.CLIENT_CREATED.name())
                .timestamp(LocalDateTime.now())
                .build()

        when: "Se consume el evento"
        clientEventConsumer.consumeClientEvent(event)

        then: "El snapshot queda persistido en la tabla cliente_snapshots"
        def snapshotOpt = clienteSnapshotRepository.findById(1L)
        snapshotOpt.isPresent()
        def snapshot = snapshotOpt.get()
        snapshot.nombre == "Jose Lema"
        snapshot.identificacion == "098254785"
        snapshot.estado == true
        snapshot.fechaActualizacion != null
    }

    def "deberia actualizar ClienteSnapshot al consumir evento CLIENT_UPDATED"() {
        given: "Un snapshot existente en base de datos"
        clienteSnapshotRepository.save(ClienteSnapshot.builder()
                .id(2L)
                .nombre("Marianela Montalvo")
                .identificacion("097548965")
                .estado(true)
                .build())

        def updateEvent = ClientEventDto.builder()
                .id(2L)
                .nombre("Marianela Montalvo de Perez")
                .identificacion("097548965")
                .estado(true)
                .eventType(ClientEventType.CLIENT_UPDATED.name())
                .timestamp(LocalDateTime.now())
                .build()

        when: "Se consume el evento de actualizacion"
        clientEventConsumer.consumeClientEvent(updateEvent)

        then: "El nombre es actualizado en el snapshot"
        def snapshotOpt = clienteSnapshotRepository.findById(2L)
        snapshotOpt.isPresent()
        snapshotOpt.get().nombre == "Marianela Montalvo de Perez"
    }

    def "deberia marcar estado inactivo y desactivar cuentas asociadas al consumir evento CLIENT_DELETED"() {
        given: "Un snapshot existente activo y una cuenta activa para el cliente"
        clienteSnapshotRepository.save(ClienteSnapshot.builder()
                .id(3L)
                .nombre("Juan Osorio")
                .identificacion("098874587")
                .estado(true)
                .build())

        cuentaRepository.save(Cuenta.builder()
                .numeroCuenta("495878")
                .tipoCuenta(TipoCuenta.AHORROS)
                .saldoInicial(new BigDecimal("100.00"))
                .saldoActual(new BigDecimal("100.00"))
                .estado(true)
                .clienteId(3L)
                .build())

        def deleteEvent = ClientEventDto.builder()
                .id(3L)
                .nombre("Juan Osorio")
                .identificacion("098874587")
                .estado(false)
                .eventType(ClientEventType.CLIENT_DELETED.name())
                .timestamp(LocalDateTime.now())
                .build()

        when: "Se consume el evento de eliminacion logica"
        clientEventConsumer.consumeClientEvent(deleteEvent)

        then: "El estado pasa a false en la tabla cliente_snapshots y las cuentas son desactivadas"
        def snapshotOpt = clienteSnapshotRepository.findById(3L)
        snapshotOpt.isPresent()
        snapshotOpt.get().estado == false

        def cuentas = cuentaRepository.findByClienteId(3L)
        !cuentas.isEmpty()
        cuentas.every { !it.estado }
    }
}
