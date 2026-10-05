package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.TipoVehiculo;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record TarifaRequest(@NotNull TipoVehiculo tipo,
                            @NotNull @PositiveOrZero Long valorHora,
                            @NotNull @PositiveOrZero Long valorFraccion,
                            @NotNull @Min(1) @Max(60) Integer fraccionMinutos) {
}
