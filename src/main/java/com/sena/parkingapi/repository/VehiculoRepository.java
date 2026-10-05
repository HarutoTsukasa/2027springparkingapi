package com.sena.parkingapi.repository;

import com.sena.parkingapi.model.Vehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findFirstByPlaca(String placa);

    /** Bloquea la fila del vehiculo para serializar entradas simultaneas de la misma placa. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from Vehiculo v where v.placa = :placa")
    Optional<Vehiculo> bloquearPorPlaca(@Param("placa") String placa);
}
