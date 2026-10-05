package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.TipoVehiculo;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EspacioRequest(@NotNull @Positive Integer numero, @NotNull TipoVehiculo tipo) {
}
