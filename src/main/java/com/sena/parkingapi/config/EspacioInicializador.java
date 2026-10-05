package com.sena.parkingapi.config;

import com.sena.parkingapi.model.EspacioParqueo;
import com.sena.parkingapi.model.TipoVehiculo;
import com.sena.parkingapi.repository.EspacioParqueoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/** Crea los espacios iniciales si la tabla esta vacia (carros, motos y bicicletas, numeracion consecutiva). */
@Slf4j
@Component
@RequiredArgsConstructor
public class EspacioInicializador implements ApplicationRunner {

    private final EspacioParqueoRepository espacioRepo;
    private final EspacioInicialProperties props;

    @Override
    public void run(ApplicationArguments args) {
        if (espacioRepo.count() > 0) {
            return;
        }
        List<EspacioParqueo> nuevos = new ArrayList<>();
        int siguiente = 1;
        siguiente = agregar(nuevos, TipoVehiculo.CARRO, props.getCarro(), siguiente);
        siguiente = agregar(nuevos, TipoVehiculo.MOTO, props.getMoto(), siguiente);
        agregar(nuevos, TipoVehiculo.BICICLETA, props.getBicicleta(), siguiente);

        espacioRepo.saveAll(nuevos);
        log.info("Espacios iniciales creados: {} carros, {} motos, {} bicicletas (total {})",
                props.getCarro(), props.getMoto(), props.getBicicleta(), nuevos.size());
    }

    private int agregar(List<EspacioParqueo> lista, TipoVehiculo tipo, int cantidad, int desde) {
        for (int i = 0; i < cantidad; i++) {
            lista.add(EspacioParqueo.builder().numero(desde + i).tipo(tipo).build());
        }
        return desde + cantidad;
    }
}
