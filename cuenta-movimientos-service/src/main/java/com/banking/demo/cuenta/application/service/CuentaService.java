package com.banking.demo.cuenta.application.service;

import com.banking.demo.cuenta.application.dto.CuentaPatchDto;
import com.banking.demo.cuenta.application.dto.CuentaRequestDto;
import com.banking.demo.cuenta.application.dto.CuentaResponseDto;
import com.banking.demo.cuenta.application.dto.CuentaUpdateDto;

import java.util.List;

public interface CuentaService {

    CuentaResponseDto create(CuentaRequestDto dto);

    List<CuentaResponseDto> findByClienteId(Long clienteId);

    CuentaResponseDto findByNumeroCuenta(String numeroCuenta);

    CuentaResponseDto update(String numeroCuenta, CuentaUpdateDto dto);

    CuentaResponseDto patch(String numeroCuenta, CuentaPatchDto dto);
}
