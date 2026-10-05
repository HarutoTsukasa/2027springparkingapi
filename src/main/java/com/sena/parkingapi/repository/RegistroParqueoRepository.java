package com.sena.parkingapi.repository;

import com.sena.parkingapi.model.EspacioParqueo;
import com.sena.parkingapi.model.RegistroParqueo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RegistroParqueoRepository extends JpaRepository<RegistroParqueo, Long> {

    boolean existsByVehiculoPlacaAndFechaSalidaIsNull(String placa);

    boolean existsByEspacio(EspacioParqueo espacio);

    List<RegistroParqueo> findAllByOrderByFechaEntradaDesc();

    List<RegistroParqueo> findByFechaSalidaIsNullOrderByFechaEntradaDesc();

    List<RegistroParqueo> findByFechaSalidaIsNotNullOrderByFechaEntradaDesc();

    List<RegistroParqueo> findByVehiculoPlacaOrderByFechaEntradaDesc(String placa);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RegistroParqueo r where r.id_registro = :id")
    Optional<RegistroParqueo> bloquearPorId(@Param("id") Long id);
}
