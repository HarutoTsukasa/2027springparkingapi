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

    // @Query explicito: las consultas derivadas "exists" buscan una propiedad llamada "id" y la entidad usa id_registro.
    @Query("select count(r) > 0 from RegistroParqueo r join r.vehiculo v where v.placa = :placa and r.fechaSalida is null")
    boolean existsByVehiculoPlacaAndFechaSalidaIsNull(@Param("placa") String placa);

    @Query("select count(r) > 0 from RegistroParqueo r where r.espacio = :espacio")
    boolean existsByEspacio(@Param("espacio") EspacioParqueo espacio);

    Optional<RegistroParqueo> findByVehiculoPlacaAndFechaSalidaIsNull(String placa);

    List<RegistroParqueo> findAllByOrderByFechaEntradaDesc();

    List<RegistroParqueo> findByFechaSalidaIsNullOrderByFechaEntradaDesc();

    List<RegistroParqueo> findByFechaSalidaIsNotNullOrderByFechaEntradaDesc();

    List<RegistroParqueo> findByVehiculoPlacaOrderByFechaEntradaDesc(String placa);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RegistroParqueo r where r.id_registro = :id")
    Optional<RegistroParqueo> bloquearPorId(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from RegistroParqueo r join r.vehiculo v where v.placa = :placa and r.fechaSalida is null")
    Optional<RegistroParqueo> bloquearActivoPorPlaca(@Param("placa") String placa);
}
