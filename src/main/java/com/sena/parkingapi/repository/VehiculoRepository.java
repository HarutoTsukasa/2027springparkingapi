package com.sena.parkingapi.repository;

import com.sena.parkingapi.model.Vehiculo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VehiculoRepository extends JpaRepository<Vehiculo, Long> {

    Optional<Vehiculo> findFirstByPlaca(String placa);
}
