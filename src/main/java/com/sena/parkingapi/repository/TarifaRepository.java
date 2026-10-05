package com.sena.parkingapi.repository;

import com.sena.parkingapi.model.Tarifa;
import com.sena.parkingapi.model.TipoVehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TarifaRepository extends JpaRepository<Tarifa, Long> {

    // @Query explicito: las consultas derivadas "exists" buscan una propiedad llamada "id" y la entidad usa id_tarifa.
    @Query("select count(t) > 0 from Tarifa t where t.tipo = :tipo and t.vigenteHasta is null")
    boolean existsByTipoAndVigenteHastaIsNull(@Param("tipo") TipoVehiculo tipo);

    Optional<Tarifa> findFirstByTipoAndVigenteHastaIsNull(TipoVehiculo tipo);

    List<Tarifa> findAllByOrderByTipoAscVigenteDesdeDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tarifa t where t.tipo = :tipo and t.vigenteHasta is null")
    Optional<Tarifa> bloquearVigente(@Param("tipo") TipoVehiculo tipo);

    @Query("select t from Tarifa t where t.tipo = :tipo and t.vigenteDesde <= :momento "
            + "and (t.vigenteHasta is null or t.vigenteHasta > :momento) order by t.vigenteDesde desc")
    List<Tarifa> vigentesEn(@Param("tipo") TipoVehiculo tipo, @Param("momento") LocalDateTime momento);
}
