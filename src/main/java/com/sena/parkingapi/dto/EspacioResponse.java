package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.EspacioParqueo;
import com.sena.parkingapi.model.EstadoEspacio;
import com.sena.parkingapi.model.TipoVehiculo;

public record EspacioResponse(Long idEspacio, Integer numero, TipoVehiculo tipo, EstadoEspacio estado) {
    public static EspacioResponse de(EspacioParqueo e) {
        return new EspacioResponse(e.getId_espacio(), e.getNumero(), e.getTipo(), e.getEstado());
    }
}
