package com.sena.parkingapi.dto;

import com.sena.parkingapi.model.TipoVehiculo;
import com.sena.parkingapi.model.Vehiculo;

public record VehiculoResponse(Long idVehiculo, String placa, TipoVehiculo tipo, String descripcion) {
    public static VehiculoResponse de(Vehiculo v) {
        return new VehiculoResponse(v.getId_vehiculo(), v.getPlaca(), v.getTipo(), v.getDescripcion());
    }
}
