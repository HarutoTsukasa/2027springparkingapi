package com.sena.parkingapi.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Cantidad de espacios que se crean automaticamente la primera vez que arranca la API
 * (solo si la tabla "espacios_parqueo" esta vacia). Por defecto: 50 en total.
 * Numeracion consecutiva: primero carros, luego motos y al final bicicletas.
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "parking.espacios.iniciales")
public class EspacioInicialProperties {

    private int carro = 25;
    private int moto = 20;
    private int bicicleta = 5;

    @PostConstruct
    void validar() {
        if (carro < 0 || moto < 0 || bicicleta < 0) {
            throw new IllegalStateException("parking.espacios.iniciales.* no puede ser negativo");
        }
    }
}
