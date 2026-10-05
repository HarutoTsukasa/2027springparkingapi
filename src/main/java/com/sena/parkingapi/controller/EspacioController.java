package com.sena.parkingapi.controller;

import com.sena.parkingapi.dto.EspacioLoteRequest;
import com.sena.parkingapi.dto.EspacioRequest;
import com.sena.parkingapi.dto.EspacioResponse;
import com.sena.parkingapi.model.EstadoEspacio;
import com.sena.parkingapi.model.TipoVehiculo;
import com.sena.parkingapi.service.EspacioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
@RequiredArgsConstructor
public class EspacioController {

    private final EspacioService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspacioResponse crear(@Valid @RequestBody EspacioRequest req) {
        return service.crear(req);
    }

    @PostMapping("/lote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<EspacioResponse> crearLote(@Valid @RequestBody EspacioLoteRequest req) {
        return service.crearLote(req);
    }

    @GetMapping
    public List<EspacioResponse> listar(@RequestParam(required = false) TipoVehiculo tipo,
                                        @RequestParam(required = false) EstadoEspacio estado) {
        return service.listar(tipo, estado);
    }

    @GetMapping("/{id}")
    public EspacioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PutMapping("/{id}")
    public EspacioResponse actualizar(@PathVariable Long id, @Valid @RequestBody EspacioRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
