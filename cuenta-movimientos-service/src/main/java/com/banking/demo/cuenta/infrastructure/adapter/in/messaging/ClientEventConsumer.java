package com.banking.demo.cuenta.infrastructure.adapter.in.messaging;

import com.banking.demo.cuenta.application.service.ClienteSnapshotService;
import com.banking.demo.event.ClientEventDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ClientEventConsumer {

    private final ClienteSnapshotService clienteSnapshotService;

    @RabbitListener(queues = "${banking.rabbitmq.queue:cuenta.client-events}")
    public void consumeClientEvent(ClientEventDto event) {
        if (event == null) {
            log.warn("Mensaje AMQP nulo recibido en cola de eventos de cliente, omitiendo");
            return;
        }

        log.info("Mensaje AMQP recibido en cola de eventos de cliente: tipo [{}] para cliente con ID [{}]",
                event.getEventType(), event.getId());

        try {
            clienteSnapshotService.sync(event);
            log.info("Evento AMQP procesado y snapshot sincronizado exitosamente para cliente con ID [{}]",
                    event.getId());
        } catch (Exception e) {
            log.error("Fallo imprevisto al procesar evento AMQP de cliente [{}]: {}",
                    event.getId(), e.getMessage(), e);
            throw e;
        }
    }
}
