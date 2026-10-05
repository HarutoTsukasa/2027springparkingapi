package com.sena.parkingapi.service;

import com.sena.parkingapi.config.TarifaProperties;
import com.sena.parkingapi.dto.CobroDetalle;
import com.sena.parkingapi.model.TipoVehiculo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Tarifa por hora y fraccion:
 *  - Las horas completas se cobran a tarifa de hora.
 *  - El tiempo sobrante se cobra por fracciones iniciadas (bloques de N minutos),
 *    sin superar nunca el valor de una hora.
 *  - Los segundos se redondean hacia arriba a minuto; el minimo cobrable es una fraccion.
 */
@Service
@RequiredArgsConstructor
public class TarifaService {

    private final TarifaProperties props;

    public CobroDetalle calcular(TipoVehiculo tipo, LocalDateTime entrada, LocalDateTime salida) {
        TarifaProperties.Valores t = props.getTipos().get(tipo);
        int fm = props.getFraccionMinutos();

        long segundos = Math.max(0, Duration.between(entrada, salida).getSeconds());
        long minutos = (segundos + 59) / 60;

        long horas = minutos / 60;
        long resto = minutos % 60;
        long fracciones = (resto + fm - 1) / fm;
        if (horas == 0 && fracciones == 0) {
            fracciones = 1;
        }

        long cobroFracciones = Math.min(fracciones * t.getFraccion(), t.getHora());
        long total = horas * t.getHora() + cobroFracciones;

        return new CobroDetalle(minutos, horas, fracciones, t.getHora(), t.getFraccion(), fm, total);
    }
}
