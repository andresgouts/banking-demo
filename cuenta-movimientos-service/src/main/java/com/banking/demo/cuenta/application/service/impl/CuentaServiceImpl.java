package com.banking.demo.cuenta.application.service.impl;

import com.banking.demo.cuenta.application.dto.CuentaPatchDto;
import com.banking.demo.cuenta.application.dto.CuentaRequestDto;
import com.banking.demo.cuenta.application.dto.CuentaResponseDto;
import com.banking.demo.cuenta.application.dto.CuentaUpdateDto;
import com.banking.demo.cuenta.application.mapper.CuentaMapper;
import com.banking.demo.cuenta.application.service.CuentaService;
import com.banking.demo.cuenta.domain.exception.DuplicateResourceException;
import com.banking.demo.cuenta.domain.exception.EntityNotFoundException;
import com.banking.demo.cuenta.domain.model.ClienteSnapshot;
import com.banking.demo.cuenta.domain.model.Cuenta;
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository;
import com.banking.demo.cuenta.domain.repository.CuentaRepository;
import com.banking.demo.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;
    private final ClienteSnapshotRepository clienteSnapshotRepository;
    private final CuentaMapper cuentaMapper;

    @Override
    @Transactional
    public CuentaResponseDto create(CuentaRequestDto dto) {
        log.info("Iniciando creacion de cuenta numero [{}] para cliente con ID [{}]",
                dto.getNumeroCuenta(), dto.getClienteId());

        validateAuthenticatedOwner(dto.getClienteId());

        if (cuentaRepository.existsByNumeroCuenta(dto.getNumeroCuenta().trim())) {
            log.warn("Conflicto al crear cuenta: ya existe la cuenta numero [{}]", dto.getNumeroCuenta());
            throw new DuplicateResourceException("Ya existe una cuenta con el numero: " + dto.getNumeroCuenta());
        }

        ClienteSnapshot snapshot = resolveClienteSnapshot(dto.getClienteId());
        log.debug("Cliente verificado en snapshot: ID [{}], Nombre [{}]",
                snapshot.getId(), snapshot.getNombre());

        Cuenta cuenta = cuentaMapper.toEntity(dto, snapshot.getId());
        Cuenta saved = cuentaRepository.save(cuenta);

        log.info("Cuenta creada exitosamente con ID [{}], numeroCuenta [{}], saldoInicial [{}]",
                saved.getId(), saved.getNumeroCuenta(), saved.getSaldoInicial());

        return cuentaMapper.toResponseDto(saved, snapshot.getNombre());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaResponseDto> findByClienteId(Long clienteId) {
        if (clienteId == null) {
            throw new EntityNotFoundException("Identificador de cliente no especificado");
        }
        validateAuthenticatedOwner(clienteId);
        log.info("Consultando cuentas asociadas al cliente con ID [{}]", clienteId);

        ClienteSnapshot snapshot = resolveClienteSnapshot(clienteId);

        List<Cuenta> cuentas = cuentaRepository.findByClienteId(snapshot.getId());
        log.debug("Se recuperaron {} cuentas para el cliente con ID [{}]", cuentas.size(), snapshot.getId());
        return cuentas.stream()
                .map(c -> cuentaMapper.toResponseDto(c, snapshot.getNombre()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaResponseDto findByNumeroCuenta(String numeroCuenta) {
        log.info("Consultando cuenta por numero: [{}]", numeroCuenta);
        Cuenta cuenta = findCuentaByNumero(numeroCuenta);
        validateAuthenticatedOwner(cuenta.getClienteId());
        return mapWithClientName(cuenta);
    }

    @Override
    @Transactional
    public CuentaResponseDto update(String numeroCuenta, CuentaUpdateDto dto) {
        log.info("Iniciando actualizacion de cuenta numero: [{}]", numeroCuenta);
        Cuenta cuenta = findCuentaByNumero(numeroCuenta);
        validateAuthenticatedOwner(cuenta.getClienteId());

        Long resolvedClienteId = null;
        String clientName = null;
        if (dto.getClienteId() != null) {
            validateAuthenticatedOwner(dto.getClienteId());
            ClienteSnapshot snapshot = resolveClienteSnapshot(dto.getClienteId());
            resolvedClienteId = snapshot.getId();
            clientName = snapshot.getNombre();
        }

        cuentaMapper.updateEntity(cuenta, dto, resolvedClienteId);
        Cuenta updated = cuentaRepository.save(cuenta);

        if (clientName == null) {
            clientName = getNombreCliente(updated.getClienteId());
        }

        log.info("Cuenta numero [{}] actualizada exitosamente", updated.getNumeroCuenta());
        return cuentaMapper.toResponseDto(updated, clientName);
    }

    @Override
    @Transactional
    public CuentaResponseDto patch(String numeroCuenta, CuentaPatchDto dto) {
        log.info("Iniciando actualizacion parcial (PATCH) de cuenta numero: [{}]", numeroCuenta);
        Cuenta cuenta = findCuentaByNumero(numeroCuenta);
        validateAuthenticatedOwner(cuenta.getClienteId());

        Long resolvedClienteId = null;
        String clientName = null;
        if (dto.getClienteId() != null) {
            validateAuthenticatedOwner(dto.getClienteId());
            ClienteSnapshot snapshot = resolveClienteSnapshot(dto.getClienteId());
            resolvedClienteId = snapshot.getId();
            clientName = snapshot.getNombre();
        }

        cuentaMapper.patchEntity(cuenta, dto, resolvedClienteId);
        Cuenta updated = cuentaRepository.save(cuenta);

        if (clientName == null) {
            clientName = getNombreCliente(updated.getClienteId());
        }

        log.info("Cuenta numero [{}] actualizada parcialmente con exito", updated.getNumeroCuenta());
        return cuentaMapper.toResponseDto(updated, clientName);
    }

    private Cuenta findCuentaByNumero(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new EntityNotFoundException("Numero de cuenta no especificado");
        }
        return cuentaRepository.findByNumeroCuenta(numeroCuenta.trim())
                .orElseThrow(() -> {
                    log.warn("Cuenta no encontrada para el numero [{}]", numeroCuenta);
                    return new EntityNotFoundException("No se encontro la cuenta con el numero: " + numeroCuenta);
                });
    }

    private ClienteSnapshot resolveClienteSnapshot(Long id) {
        if (id == null) {
            throw new EntityNotFoundException("Identificador de cliente no especificado");
        }
        return clienteSnapshotRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Cliente con ID [{}] no encontrado en la replica local de clientes", id);
                    return new EntityNotFoundException("No se encontro el cliente con identificador: " + id +
                            " en el registro de clientes sincronizados");
                });
    }

    private void validateAuthenticatedOwner(Long ownerId) {
        SecurityUtils.validateAuthenticatedOwner(ownerId);
    }

    private CuentaResponseDto mapWithClientName(Cuenta cuenta) {
        String clientName = getNombreCliente(cuenta.getClienteId());
        return cuentaMapper.toResponseDto(cuenta, clientName);
    }

    private String getNombreCliente(Long clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clienteSnapshotRepository.findById(clienteId)
                .map(ClienteSnapshot::getNombre)
                .orElse(null);
    }
}
