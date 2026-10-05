package com.sena.parkingapi.config;

import com.sena.parkingapi.model.TipoVehiculo;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Valores INICIALES de tarifa (pesos COP). Solo se usan para sembrar la tabla "tarifas"
 * cuando un tipo de vehiculo no tiene tarifa vigente. Despues se administran por /api/tarifas.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "parking.tarifa")
public class TarifaProperties {

    /** Duracion en minutos de una fraccion (1 a 60). */
    private int fraccionMinutos = 15;

    private Map<TipoVehiculo, Valores> tipos = new HashMap<>();

    @Getter
    @Setter
    public static class Valores {
        private long hora;
        private long fraccion;
    }

    @PostConstruct
    void validar() {
        if (fraccionMinutos < 1 || fraccionMinutos > 60) {
            throw new IllegalStateException("parking.tarifa.fraccion-minutos debe estar entre 1 y 60");
        }
        for (TipoVehiculo t : TipoVehiculo.values()) {
            Valores v = tipos.get(t);
            if (v == null || v.getHora() < 0 || v.getFraccion() < 0) {
                throw new IllegalStateException("Falta o es invalida la tarifa parking.tarifa.tipos." + t.name().toLowerCase());
            }
        }
    }
}
