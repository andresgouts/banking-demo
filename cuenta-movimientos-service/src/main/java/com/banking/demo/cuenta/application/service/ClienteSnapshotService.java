package com.banking.demo.cuenta.application.service;

import com.banking.demo.cuenta.domain.model.ClienteSnapshot;
import com.banking.demo.event.ClientEventDto;

import java.util.Optional;

public interface ClienteSnapshotService {

    ClienteSnapshot sync(ClientEventDto event);

    Optional<ClienteSnapshot> findById(Long id);

    Optional<ClienteSnapshot> findByIdentificacion(String identificacion);
}
