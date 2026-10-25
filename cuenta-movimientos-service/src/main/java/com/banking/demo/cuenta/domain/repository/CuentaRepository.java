package com.banking.demo.cuenta.domain.repository;

import com.banking.demo.cuenta.domain.model.Cuenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CuentaRepository extends JpaRepository<Cuenta, Long> {

    Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Cuenta c WHERE c.numeroCuenta = :numeroCuenta")
    Optional<Cuenta> findByNumeroCuentaForUpdate(@Param("numeroCuenta") String numeroCuenta);

    List<Cuenta> findByClienteId(Long clienteId);

    boolean existsByNumeroCuenta(String numeroCuenta);
}
