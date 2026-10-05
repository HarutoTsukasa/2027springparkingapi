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

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehiculoController {

    private final RegistroService service;

    @GetMapping
    public List<VehiculoResponse> listar() {
        return service.listarVehiculos();
    }

    @GetMapping("/{placa}/registros")
    public List<RegistroResponse> historial(@PathVariable String placa) {
        return service.historialPorPlaca(placa);
    }
}
