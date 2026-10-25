package com.banking.demo.cliente.application.service.impl;

import com.banking.demo.cliente.application.dto.ClientePatchDto;
import com.banking.demo.cliente.application.dto.ClienteRequestDto;
import com.banking.demo.cliente.application.dto.ClienteResponseDto;
import com.banking.demo.cliente.application.dto.ClienteUpdateDto;
import com.banking.demo.cliente.application.mapper.ClienteMapper;
import com.banking.demo.cliente.application.port.out.ClientEventPublisherPort;
import com.banking.demo.cliente.application.service.ClienteService;
import com.banking.demo.cliente.domain.exception.DuplicateResourceException;
import com.banking.demo.cliente.domain.exception.EntityNotFoundException;
import com.banking.demo.cliente.domain.model.Cliente;
import com.banking.demo.cliente.domain.repository.ClienteRepository;
import com.banking.demo.event.ClientEventDto;
import com.banking.demo.event.ClientEventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClienteServiceImpl implements ClienteService {

    private final ClienteRepository clienteRepository;
    private final ClienteMapper clienteMapper;
    private final PasswordEncoder passwordEncoder;
    private final ClientEventPublisherPort clientEventPublisher;

    @Override
    @Transactional
    public ClienteResponseDto create(ClienteRequestDto dto) {
        log.info("Iniciando creación de nuevo cliente con identificación: [{}]", dto.getIdentificacion());

        if (clienteRepository.existsByIdentificacion(dto.getIdentificacion())) {
            log.warn("Conflicto al crear cliente: ya existe la identificación [{}]", dto.getIdentificacion());
            throw new DuplicateResourceException("Ya existe un cliente con la identificación: " + dto.getIdentificacion());
        }

        log.debug("Mapeando DTO a entidad Cliente");
        Cliente cliente = clienteMapper.toEntity(dto, passwordEncoder);
        Cliente saved = clienteRepository.save(cliente);

        log.info("Cliente creado exitosamente con ID [{}], identificación [{}]",
                saved.getId(), saved.getIdentificacion());

        // Emitir evento asíncrono
        ClientEventDto event = ClientEventDto.builder()
                .id(saved.getId())
                .nombre(saved.getNombre())
                .identificacion(saved.getIdentificacion())
                .estado(saved.getEstado())
                .eventType(ClientEventType.CLIENT_CREATED.name())
                .build();
        clientEventPublisher.publishClientCreated(event);

        return clienteMapper.toResponseDto(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClienteResponseDto> findAll() {
        log.info("Consultando listado general de clientes");
        List<Cliente> clientes = clienteRepository.findAll();
        log.debug("Se recuperaron {} registros de clientes", clientes.size());
        return clientes.stream()
                .map(clienteMapper::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ClienteResponseDto findById(Long id) {
        log.info("Consultando cliente por ID: [{}]", id);
        Cliente cliente = findClienteById(id);
        return clienteMapper.toResponseDto(cliente);
    }

    @Override
    @Transactional
    public ClienteResponseDto update(Long id, ClienteUpdateDto dto) {
        log.info("Iniciando actualización total del cliente con ID: [{}]", id);
        Cliente cliente = findClienteById(id);

        if (!cliente.getIdentificacion().equals(dto.getIdentificacion()) &&
                clienteRepository.existsByIdentificacion(dto.getIdentificacion())) {
            log.warn("Conflicto al actualizar cliente: la identificación [{}] ya pertenece a otro cliente", dto.getIdentificacion());
            throw new DuplicateResourceException("Ya existe otro cliente con la identificación: " + dto.getIdentificacion());
        }

        log.debug("Actualizando datos del cliente [{}]", cliente.getId());
        clienteMapper.updateEntity(cliente, dto, passwordEncoder);
        Cliente updated = clienteRepository.save(cliente);

        log.info("Cliente [{}] actualizado exitosamente", updated.getId());

        // Emitir evento asíncrono
        ClientEventDto event = ClientEventDto.builder()
                .id(updated.getId())
                .nombre(updated.getNombre())
                .identificacion(updated.getIdentificacion())
                .estado(updated.getEstado())
                .eventType(ClientEventType.CLIENT_UPDATED.name())
                .build();
        clientEventPublisher.publishClientUpdated(event);

        return clienteMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public ClienteResponseDto patch(Long id, ClientePatchDto dto) {
        log.info("Iniciando actualización parcial (PATCH) del cliente con ID: [{}]", id);
        Cliente cliente = findClienteById(id);

        log.debug("Aplicando cambios parciales al cliente [{}]", cliente.getId());
        clienteMapper.patchEntity(cliente, dto, passwordEncoder);
        Cliente updated = clienteRepository.save(cliente);

        log.info("Cliente [{}] actualizado parcialmente de forma exitosa", updated.getId());

        // Emitir evento asíncrono
        ClientEventDto event = ClientEventDto.builder()
                .id(updated.getId())
                .nombre(updated.getNombre())
                .identificacion(updated.getIdentificacion())
                .estado(updated.getEstado())
                .eventType(ClientEventType.CLIENT_UPDATED.name())
                .build();
        clientEventPublisher.publishClientUpdated(event);

        return clienteMapper.toResponseDto(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        log.info("Iniciando deshabilitación lógica (soft delete) del cliente con ID: [{}]", id);
        Cliente cliente = findClienteById(id);
        cliente.setEstado(Boolean.FALSE);
        cliente.setFechaEliminacion(LocalDateTime.now());
        Cliente updated = clienteRepository.save(cliente);
        log.info("Cliente [{}] deshabilitado exitosamente con fecha de eliminación [{}]",
                updated.getId(), updated.getFechaEliminacion());

        // Emitir evento asíncrono
        ClientEventDto event = ClientEventDto.builder()
                .id(updated.getId())
                .nombre(updated.getNombre())
                .identificacion(updated.getIdentificacion())
                .estado(updated.getEstado())
                .eventType(ClientEventType.CLIENT_DELETED.name())
                .build();
        clientEventPublisher.publishClientDeleted(event);
    }

    private Cliente findClienteById(Long id) {
        if (id == null) {
            throw new EntityNotFoundException("ID de cliente no especificado");
        }
        return clienteRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cliente no encontrado con ID [{}]", id);
                    return new EntityNotFoundException("Cliente no encontrado con ID: " + id);
                });
    }
}
