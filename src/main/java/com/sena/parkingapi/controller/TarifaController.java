package com.sena.parkingapi.controller;

import com.sena.parkingapi.dto.TarifaRequest;
import com.sena.parkingapi.dto.TarifaResponse;
import com.sena.parkingapi.model.TipoVehiculo;
import com.sena.parkingapi.service.TarifaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
public class TarifaController {

    private final TarifaService service;

    /** Crea una nueva tarifa vigente para el tipo; la anterior queda cerrada en el historial. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarifaResponse crear(@Valid @RequestBody TarifaRequest req) {
        return service.crear(req);
    }

    @GetMapping
    public List<TarifaResponse> listar(@RequestParam(required = false) TipoVehiculo tipo,
                                       @RequestParam(required = false) Boolean vigentes) {
        return service.listar(tipo, vigentes);
    }

    @GetMapping("/{id}")
    public TarifaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
