package com.banking.demo.cuenta.application.service.impl;

import com.banking.demo.cuenta.application.service.ClienteSnapshotService;
import com.banking.demo.cuenta.domain.model.ClienteSnapshot;
import com.banking.demo.cuenta.domain.model.Cuenta;
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository;
import com.banking.demo.cuenta.domain.repository.CuentaRepository;
import com.banking.demo.event.ClientEventDto;
import com.banking.demo.event.ClientEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteSnapshotServiceImpl implements ClienteSnapshotService {

    private final ClienteSnapshotRepository clienteSnapshotRepository;
    private final CuentaRepository cuentaRepository;

    @Override
    @Transactional
    public ClienteSnapshot sync(ClientEventDto event) {
        log.info("Iniciando sincronizacion de snapshot para cliente con ID [{}] con evento [{}]",
                event.getId(), event.getEventType());
        log.debug("Datos del evento recibido: {}", event);

        if (event.getId() == null) {
            log.warn("Evento recibido con id nulo, no se puede sincronizar");
            throw new IllegalArgumentException("El id del cliente no puede ser nulo para sincronizar snapshot");
        }

        Optional<ClienteSnapshot> existingOpt = clienteSnapshotRepository.findById(event.getId());

        ClienteSnapshot snapshot;
        if (existingOpt.isPresent()) {
            snapshot = existingOpt.get();
            log.debug("Actualizando snapshot existente para cliente con ID [{}]", event.getId());
            if (event.getNombre() != null && !event.getNombre().isBlank()) {
                snapshot.setNombre(event.getNombre().trim());
            }
            if (event.getIdentificacion() != null && !event.getIdentificacion().isBlank()) {
                snapshot.setIdentificacion(event.getIdentificacion().trim());
            }
            if (ClientEventType.CLIENT_DELETED.name().equalsIgnoreCase(event.getEventType())) {
                snapshot.setEstado(Boolean.FALSE);
            } else if (event.getEstado() != null) {
                snapshot.setEstado(event.getEstado());
            }
        } else {
            log.debug("Creando nuevo snapshot para cliente con ID [{}]", event.getId());
            Boolean estado = ClientEventType.CLIENT_DELETED.name().equalsIgnoreCase(event.getEventType())
                    ? Boolean.FALSE
                    : (event.getEstado() != null ? event.getEstado() : Boolean.TRUE);

            String nombre = (event.getNombre() != null && !event.getNombre().isBlank())
                    ? event.getNombre().trim()
                    : "SIN_NOMBRE";

            String identificacion = (event.getIdentificacion() != null && !event.getIdentificacion().isBlank())
                    ? event.getIdentificacion().trim()
                    : "SIN_IDENTIFICACION";

            snapshot = ClienteSnapshot.builder()
                    .id(event.getId())
                    .nombre(nombre)
                    .identificacion(identificacion)
                    .estado(estado)
                    .build();
        }

        ClienteSnapshot saved = clienteSnapshotRepository.save(snapshot);
        log.info("Snapshot de cliente [{}] sincronizado exitosamente con identificacion [{}] y estado [{}]",
                saved.getId(), saved.getIdentificacion(), saved.getEstado());

        if (ClientEventType.CLIENT_DELETED.name().equalsIgnoreCase(event.getEventType())) {
            desactivarCuentasDeCliente(event.getId());
        }

        return saved;
    }

    private void desactivarCuentasDeCliente(Long clienteId) {
        log.info("Desactivando cuentas asociadas al cliente con ID [{}] tras evento CLIENT_DELETED", clienteId);
        List<Cuenta> cuentas = cuentaRepository.findByClienteId(clienteId);
        if (cuentas == null || cuentas.isEmpty()) {
            log.debug("No se encontraron cuentas asociadas al cliente con ID [{}] para desactivar", clienteId);
            return;
        }

        List<Cuenta> cuentasADesactivar = cuentas.stream()
                .filter(c -> !Boolean.FALSE.equals(c.getEstado()))
                .toList();

        if (!cuentasADesactivar.isEmpty()) {
            cuentasADesactivar.forEach(c -> c.setEstado(Boolean.FALSE));
            cuentaRepository.saveAll(cuentasADesactivar);
            log.info("Se desactivaron [{}] cuentas asociadas al cliente con ID [{}] tras evento CLIENT_DELETED",
                    cuentasADesactivar.size(), clienteId);
        } else {
            log.debug("Todas las cuentas ({}) del cliente con ID [{}] ya se encontraban inactivas", cuentas.size(), clienteId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteSnapshot> findById(Long id) {
        log.debug("Buscando snapshot de cliente por id [{}]", id);
        return clienteSnapshotRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteSnapshot> findByIdentificacion(String identificacion) {
        log.debug("Buscando snapshot de cliente por identificacion [{}]", identificacion);
        return clienteSnapshotRepository.findByIdentificacion(identificacion);
    }
}
