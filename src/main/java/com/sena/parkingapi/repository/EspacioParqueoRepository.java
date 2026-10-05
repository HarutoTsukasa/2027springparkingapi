package com.sena.parkingapi.repository;

import com.sena.parkingapi.model.EspacioParqueo;
import com.sena.parkingapi.model.EstadoEspacio;
import com.sena.parkingapi.model.TipoVehiculo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EspacioParqueoRepository extends JpaRepository<EspacioParqueo, Long> {

    boolean existsByNumero(Integer numero);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<EspacioParqueo> findFirstByTipoAndEstadoOrderByNumeroAsc(TipoVehiculo tipo, EstadoEspacio estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from EspacioParqueo e where e.numero = :numero")
    Optional<EspacioParqueo> bloquearPorNumero(@Param("numero") Integer numero);
}
