package com.banking.demo.cuenta.application.service.impl;

import com.banking.demo.cuenta.application.dto.MovimientoRequestDto;
import com.banking.demo.cuenta.application.dto.MovimientoResponseDto;
import com.banking.demo.cuenta.application.mapper.MovimientoMapper;
import com.banking.demo.cuenta.application.service.MovimientoService;
import com.banking.demo.cuenta.domain.exception.EntityNotFoundException;
import com.banking.demo.cuenta.domain.exception.InsufficientBalanceException;
import com.banking.demo.cuenta.domain.model.ClienteSnapshot;
import com.banking.demo.cuenta.domain.model.Cuenta;
import com.banking.demo.cuenta.domain.model.Movimiento;
import com.banking.demo.cuenta.domain.model.TipoMovimiento;
import com.banking.demo.cuenta.domain.repository.ClienteSnapshotRepository;
import com.banking.demo.cuenta.domain.repository.CuentaRepository;
import com.banking.demo.cuenta.domain.repository.MovimientoRepository;
import com.banking.demo.security.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MovimientoServiceImpl implements MovimientoService {

    private final MovimientoRepository movimientoRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteSnapshotRepository clienteSnapshotRepository;
    private final MovimientoMapper movimientoMapper;

    @Override
    @Transactional
    public MovimientoResponseDto registrarMovimiento(MovimientoRequestDto dto) {
        log.info("Iniciando registro de transaccion para cuenta [{}], valor: [{}]",
                dto.getNumeroCuenta(), dto.getValor());

        if (dto.getNumeroCuenta() == null || dto.getNumeroCuenta().isBlank()) {
            throw new IllegalArgumentException("El numero de cuenta es obligatorio");
        }

        if (dto.getValor() == null || dto.getValor().compareTo(BigDecimal.ZERO) == 0) {
            log.warn("Rechazo de transaccion: El valor del movimiento no puede ser cero ni nulo");
            throw new IllegalArgumentException("El valor del movimiento no puede ser cero ni nulo");
        }

        BigDecimal valor = dto.getValor();

        // 1. Deducir tipo de movimiento a partir del signo del valor
        TipoMovimiento tipoMovimiento = valor.compareTo(BigDecimal.ZERO) > 0
                ? TipoMovimiento.DEPOSITO
                : TipoMovimiento.RETIRO;

        log.debug("Tipo de movimiento determinado automaticamente por valor [{}]: [{}]", valor, tipoMovimiento);

        // 2. Obtener cuenta aplicando bloqueo pesimista (PESSIMISTIC_WRITE) para control de concurrencia
        String cleanNumeroCuenta = dto.getNumeroCuenta().trim();
        Cuenta cuenta = cuentaRepository.findByNumeroCuentaForUpdate(cleanNumeroCuenta)
                .orElseThrow(() -> {
                    log.warn("Cuenta no encontrada para registro de movimiento: [{}]", cleanNumeroCuenta);
                    return new EntityNotFoundException("No se encontro la cuenta con el numero: " + cleanNumeroCuenta);
                });

        // 3. Validar pertenencia de la cuenta al usuario autenticado por Id
        validateAuthenticatedOwner(cuenta.getClienteId());

        if (Boolean.FALSE.equals(cuenta.getEstado())) {
            log.warn("Intento de transaccion sobre cuenta inactiva: [{}]", cleanNumeroCuenta);
            throw new IllegalArgumentException("La cuenta numero " + cleanNumeroCuenta + " se encuentra inactiva");
        }

        BigDecimal saldoActual = cuenta.getSaldoActual();
        log.debug("Saldo previo de la cuenta [{}]: [{}]", cleanNumeroCuenta, saldoActual);

        // 4. Validar regla de saldo insuficiente:
        // Si valor < 0 (retiro) y saldoActual + valor < 0 -> Saldo no disponible
        BigDecimal saldoDisponible = saldoActual.add(valor);
        if (saldoDisponible.compareTo(BigDecimal.ZERO) < 0) {
            log.warn("Saldo insuficiente en cuenta [{}]. Saldo actual: [{}], Monto a debitar: [{}]",
                    cleanNumeroCuenta, saldoActual, valor);
            throw new InsufficientBalanceException("Saldo no disponible");
        }

        // 5. Actualizar saldo actual de la cuenta
        cuenta.setSaldoActual(saldoDisponible);
        cuentaRepository.save(cuenta);
        log.debug("Saldo actualizado de la cuenta [{}]: [{}]", cleanNumeroCuenta, saldoDisponible);

        // 6. Guardar registro inmutable en movimiento
        Movimiento movimiento = movimientoMapper.toEntity(dto, cuenta, tipoMovimiento, saldoDisponible);
        Movimiento saved = movimientoRepository.save(movimiento);

        String clientName = resolveClientName(cuenta.getClienteId());
        log.info("Transaccion completada exitosamente. Movimiento ID [{}], Cuenta [{}], Tipo [{}], Valor [{}], Saldo Resultante [{}]",
                saved.getId(), saved.getNumeroCuenta(), saved.getTipoMovimiento(), saved.getValor(), saved.getSaldo());

        return movimientoMapper.toResponseDto(saved, clientName);
    }

    @Override
    @Transactional(readOnly = true)
    public MovimientoResponseDto findById(Long id) {
        log.info("Consultando movimiento por ID [{}]", id);
        Movimiento movimiento = movimientoRepository.findByIdWithCuenta(id)
                .orElseThrow(() -> {
                    log.warn("Movimiento no encontrado con ID [{}]", id);
                    return new EntityNotFoundException("No se encontro el movimiento con ID: " + id);
                });

        Long ownerClienteId = movimiento.getCuenta() != null
                ? movimiento.getCuenta().getClienteId()
                : (movimiento.getNumeroCuenta() != null
                    ? cuentaRepository.findByNumeroCuenta(movimiento.getNumeroCuenta())
                        .map(Cuenta::getClienteId).orElse(null)
                    : null);

        validateAuthenticatedOwner(ownerClienteId);

        String clientName = ownerClienteId != null
                ? resolveClientName(ownerClienteId)
                : null;

        return movimientoMapper.toResponseDto(movimiento, clientName);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponseDto> findByNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new IllegalArgumentException("El numero de cuenta no puede ser vacio");
        }
        String cleanNumeroCuenta = numeroCuenta.trim();
        log.info("Consultando movimientos de la cuenta [{}]", cleanNumeroCuenta);

        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(cleanNumeroCuenta)
                .orElseThrow(() -> {
                    log.warn("No existe la cuenta [{}] al consultar movimientos", cleanNumeroCuenta);
                    return new EntityNotFoundException("No se encontro la cuenta con el numero: " + cleanNumeroCuenta);
                });

        validateAuthenticatedOwner(cuenta.getClienteId());

        List<Movimiento> movimientos = movimientoRepository.findByNumeroCuentaWithCuenta(cleanNumeroCuenta);
        log.debug("Se recuperaron {} movimientos para la cuenta [{}]", movimientos.size(), cleanNumeroCuenta);
        return movimientos.stream()
                .map(this::mapWithClientName)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoResponseDto> findByClienteId(Long clienteId) {
        if (clienteId == null) {
            throw new EntityNotFoundException("Identificador de cliente no especificado");
        }
        validateAuthenticatedOwner(clienteId);
        log.info("Consultando movimientos asociados al cliente con ID [{}]", clienteId);

        List<Movimiento> movimientos = movimientoRepository.findByClienteIdWithCuenta(clienteId);
        log.debug("Se recuperaron {} movimientos para el cliente con ID [{}]", movimientos.size(), clienteId);
        return movimientos.stream()
                .map(this::mapWithClientName)
                .toList();
    }

    private void validateAuthenticatedOwner(Long ownerClienteId) {
        SecurityUtils.validateAuthenticatedOwner(ownerClienteId);
    }

    private MovimientoResponseDto mapWithClientName(Movimiento movimiento) {
        String clientName = null;
        if (movimiento.getCuenta() != null && movimiento.getCuenta().getClienteId() != null) {
            clientName = resolveClientName(movimiento.getCuenta().getClienteId());
        }
        return movimientoMapper.toResponseDto(movimiento, clientName);
    }

    private String resolveClientName(Long clienteId) {
        if (clienteId == null) {
            return null;
        }
        return clienteSnapshotRepository.findById(clienteId)
                .map(ClienteSnapshot::getNombre)
                .orElse(null);
    }
}
