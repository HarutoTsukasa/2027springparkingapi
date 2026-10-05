package com.sena.parkingapi.controller;

import com.sena.parkingapi.dto.RegistroResponse;
import com.sena.parkingapi.dto.VehiculoResponse;
import com.sena.parkingapi.service.RegistroService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Consulta de vehiculos conocidos. Base: {@code /api/vehiculos}.
 * Los vehiculos se crean automaticamente al registrar su primera entrada.
 */
@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final RegistroService service;

    /**
     * {@code GET /api/vehiculos} - Lista todos los vehiculos que han ingresado alguna vez.
     *
     * @return 200 con la lista (vacia si aun no hay vehiculos)
     */
    @GetMapping
    public List<VehiculoResponse> listar() {
        return service.listarVehiculos();
    }

    /**
     * {@code GET /api/vehiculos/{placa}/registros} - Historial completo de entradas y salidas de una placa,
     * del mas reciente al mas antiguo.
     *
     * @param placa placa del vehiculo; se normaliza (mayusculas, sin guiones ni espacios)
     * @return 200 con la lista de registros (vacia si la placa no tiene historial); 400 si la placa es invalida
     */
    @GetMapping("/{placa}/registros")
    public List<RegistroResponse> historial(@PathVariable String placa) {
        return service.historialPorPlaca(placa);
    }
}
