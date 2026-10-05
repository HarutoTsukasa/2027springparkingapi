package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.Tarifa;
import com.sena.parkingapi.model.TipoVehiculo;

import java.time.LocalDateTime;

public record TarifaResponse(Long idTarifa, TipoVehiculo tipo, Long valorHora, Long valorFraccion,
                             Integer fraccionMinutos, LocalDateTime vigenteDesde, LocalDateTime vigenteHasta,
                             boolean vigente) {
    public static TarifaResponse de(Tarifa t) {
        return new TarifaResponse(t.getId_tarifa(), t.getTipo(), t.getValorHora(), t.getValorFraccion(),
                t.getFraccionMinutos(), t.getVigenteDesde(), t.getVigenteHasta(), t.getVigenteHasta() == null);
    }
}
