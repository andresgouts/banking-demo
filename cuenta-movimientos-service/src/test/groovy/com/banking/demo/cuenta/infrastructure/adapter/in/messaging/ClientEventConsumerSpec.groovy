package com.banking.demo.cuenta.infrastructure.adapter.in.messaging

import com.banking.demo.cuenta.application.service.ClienteSnapshotService
import com.banking.demo.event.ClientEventDto
import com.banking.demo.event.ClientEventType
import spock.lang.Specification
import spock.lang.Subject

import java.time.LocalDateTime

class ClientEventConsumerSpec extends Specification {

    ClienteSnapshotService clienteSnapshotService = Mock()

    @Subject
    ClientEventConsumer clientEventConsumer = new ClientEventConsumer(clienteSnapshotService)

    def "deberia procesar evento AMQP delegando la sincronizacion al servicio"() {
        given: "Un evento AMQP de creacion de cliente"
        def event = ClientEventDto.builder()
                .id(1L)
                .nombre("Jose Lema")
                .identificacion("098254785")
                .estado(true)
                .eventType(ClientEventType.CLIENT_CREATED.name())
                .timestamp(LocalDateTime.now())
                .build()

        when: "El consumidor recibe el evento"
        clientEventConsumer.consumeClientEvent(event)

        then: "Se delega la sincronizacion a ClienteSnapshotService"
        1 * clienteSnapshotService.sync(event)
    }

    def "deberia ignorar silenciosamente eventos nulos sin lanzar excepcion"() {
        when: "Se recibe un evento nulo"
        clientEventConsumer.consumeClientEvent(null)

        then: "No se lanza excepcion y no se invoca sincronizacion"
        notThrown(Exception)
        0 * clienteSnapshotService.sync(_)
    }

    def "deberia propagar la excepcion si el servicio de sincronizacion falla"() {
        given: "Un evento con fallo simulado en base de datos"
        def event = ClientEventDto.builder()
                .id(99L)
                .eventType(ClientEventType.CLIENT_CREATED.name())
                .build()

        clienteSnapshotService.sync(event) >> { throw new RuntimeException("Error en base de datos") }

        when: "El consumidor procesa el evento y el servicio falla"
        clientEventConsumer.consumeClientEvent(event)

        then: "La excepcion es propagada"
        def ex = thrown(RuntimeException)
        ex.message == "Error en base de datos"
    }
}
