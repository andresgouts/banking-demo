package com.banking.demo.cuenta.domain.repository;

import com.banking.demo.cuenta.domain.model.Movimiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    List<Movimiento> findByNumeroCuentaOrderByFechaDesc(String numeroCuenta);

    @Query("SELECT m FROM Movimiento m LEFT JOIN FETCH m.cuenta WHERE m.numeroCuenta = :numeroCuenta ORDER BY m.fecha DESC")
    List<Movimiento> findByNumeroCuentaWithCuenta(@Param("numeroCuenta") String numeroCuenta);

    @Query("SELECT m FROM Movimiento m LEFT JOIN FETCH m.cuenta WHERE m.id = :id")
    Optional<Movimiento> findByIdWithCuenta(@Param("id") Long id);

    @Query("SELECT m FROM Movimiento m LEFT JOIN FETCH m.cuenta c WHERE c.clienteId = :clienteId ORDER BY m.fecha DESC, m.id DESC")
    List<Movimiento> findByClienteIdWithCuenta(@Param("clienteId") Long clienteId);

    List<Movimiento> findByCuentaIdOrderByFechaDesc(Long cuentaId);

    List<Movimiento> findByNumeroCuentaAndFechaBetweenOrderByFechaDesc(
            String numeroCuenta, LocalDateTime fechaInicio, LocalDateTime fechaFin);

    @Query("SELECT m FROM Movimiento m LEFT JOIN FETCH m.cuenta c WHERE c.clienteId = :clienteId AND m.fecha BETWEEN :fechaInicio AND :fechaFin ORDER BY m.fecha DESC, m.id DESC")
    List<Movimiento> findByClienteIdAndFechaBetween(
            @Param("clienteId") Long clienteId,
            @Param("fechaInicio") LocalDateTime fechaInicio,
            @Param("fechaFin") LocalDateTime fechaFin);

    List<Movimiento> findByCuentaClienteIdAndFechaBetweenOrderByFechaDesc(
            Long clienteId, LocalDateTime fechaInicio, LocalDateTime fechaFin);
}
