package com.sena.parkingapi.controller;

import com.sena.parkingapi.dto.EntradaRequest;
import com.sena.parkingapi.dto.RegistroResponse;
import com.sena.parkingapi.service.RegistroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/registros")
@RequiredArgsConstructor
public class RegistroController {

    private final RegistroService service;

    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse entrada(@Valid @RequestBody EntradaRequest req) {
        return service.registrarEntrada(req);
    }

    @PostMapping("/{id}/salida")
    public RegistroResponse salida(@PathVariable Long id) {
        return service.registrarSalida(id);
    }

    @GetMapping("/{id}/cobro")
    public RegistroResponse cobro(@PathVariable Long id) {
        return service.consultarCobro(id);
    }

    @GetMapping("/{id}")
    public RegistroResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping
    public List<RegistroResponse> listar(@RequestParam(required = false) Boolean activos) {
        return service.listar(activos);
    }
}
