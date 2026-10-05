package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.RegistroParqueo;
import com.sena.parkingapi.model.TipoVehiculo;

import java.time.LocalDateTime;

public record RegistroResponse(Long idRegistro, String placa, TipoVehiculo tipoVehiculo, Integer numeroEspacio,
                               LocalDateTime fechaEntrada, LocalDateTime fechaSalida, Double valorPagado,
                               CobroDetalle cobro) {
    public static RegistroResponse de(RegistroParqueo r, CobroDetalle cobro) {
        return new RegistroResponse(r.getId_registro(), r.getVehiculo().getPlaca(), r.getVehiculo().getTipo(),
                r.getEspacio().getNumero(), r.getFechaEntrada(), r.getFechaSalida(), r.getValorPagado(), cobro);
    }
}
