package com.sena.parkingapi.config;

import com.sena.parkingapi.model.Tarifa;
import com.sena.parkingapi.model.TipoVehiculo;
import com.sena.parkingapi.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/** Siembra una tarifa vigente por tipo (desde application.properties) si no existe ninguna. */
@Slf4j
@Component
@RequiredArgsConstructor
public class TarifaInicializador implements ApplicationRunner {

    private final TarifaRepository tarifaRepo;
    private final TarifaProperties props;

    @Override
    public void run(ApplicationArguments args) {
        for (TipoVehiculo tipo : TipoVehiculo.values()) {
            if (tarifaRepo.existsByTipoAndVigenteHastaIsNull(tipo)) {
                continue;
            }
            TarifaProperties.Valores v = props.getTipos().get(tipo);
            tarifaRepo.save(Tarifa.builder()
                    .tipo(tipo)
                    .valorHora(v.getHora())
                    .valorFraccion(v.getFraccion())
                    .fraccionMinutos(props.getFraccionMinutos())
                    .vigenteDesde(ZonaHoraria.ahora())
                    .build());
            log.info("Tarifa inicial creada para {}", tipo);
        }
    }
}
