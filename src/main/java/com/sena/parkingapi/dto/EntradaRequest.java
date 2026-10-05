package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.TipoVehiculo;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * placa obligatoria para todos los tipos (para bicicletas, un identificador de 5 a 7 caracteres, ej. serial).
 * numeroEspacio es opcional (si falta, se asigna el primer libre).
 */
public record EntradaRequest(@NotBlank String placa, @NotNull TipoVehiculo tipo, @Size(max = 1000) String descripcion,
                             @Positive Integer numeroEspacio) {
}
