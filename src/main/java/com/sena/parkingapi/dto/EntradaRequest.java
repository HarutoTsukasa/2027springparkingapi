package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.TipoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/** placa es obligatoria para CARRO y MOTO; opcional para BICICLETA. numeroEspacio es opcional (si falta, se asigna el primer libre). */
public record EntradaRequest(String placa, @NotNull TipoVehiculo tipo, @Size(max = 255) String descripcion,
                             @Positive Integer numeroEspacio) {
}
