package com.banking.demo.cuenta.domain.repository;

import com.banking.demo.cuenta.domain.model.ClienteSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClienteSnapshotRepository extends JpaRepository<ClienteSnapshot, Long> {

    Optional<ClienteSnapshot> findByIdentificacion(String identificacion);
}
