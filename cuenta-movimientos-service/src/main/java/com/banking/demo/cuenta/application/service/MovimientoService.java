package com.banking.demo.cuenta.application.service;

import com.banking.demo.cuenta.application.dto.MovimientoRequestDto;
import com.banking.demo.cuenta.application.dto.MovimientoResponseDto;

import java.util.List;

public interface MovimientoService {

    MovimientoResponseDto registrarMovimiento(MovimientoRequestDto dto);

    MovimientoResponseDto findById(Long id);

    List<MovimientoResponseDto> findByNumeroCuenta(String numeroCuenta);

    List<MovimientoResponseDto> findByClienteId(Long clienteId);
}
