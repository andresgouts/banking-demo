package com.banking.demo.cliente.application.service;

import com.banking.demo.cliente.application.dto.ClientePatchDto;
import com.banking.demo.cliente.application.dto.ClienteRequestDto;
import com.banking.demo.cliente.application.dto.ClienteResponseDto;
import com.banking.demo.cliente.application.dto.ClienteUpdateDto;

import java.util.List;

public interface ClienteService {

    ClienteResponseDto create(ClienteRequestDto dto);

    List<ClienteResponseDto> findAll();

    ClienteResponseDto findById(Long id);

    ClienteResponseDto update(Long id, ClienteUpdateDto dto);

    ClienteResponseDto patch(Long id, ClientePatchDto dto);

    void delete(Long id);
}
