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

/**
 * Administracion de tarifas por hora y fraccion (valores en pesos COP).
 * Base: {@code /api/tarifas}. Las tarifas forman un historial: no se editan ni se eliminan;
 * crear una nueva cierra la vigente del mismo tipo. Al arrancar se siembra una tarifa vigente por tipo.
 */
@RestController
@RequestMapping("/api/tarifas")
@RequiredArgsConstructor
public class TarifaController {

    private final TarifaService service;

    /**
     * {@code POST /api/tarifas} - Crea la nueva tarifa vigente de un tipo de vehiculo y cierra la anterior
     * (su vigenteHasta pasa a ser el momento actual). Se cobra con la vigente en el momento de la salida.
     *
     * @param req tipo, valorHora, valorFraccion (&gt;= 0) y fraccionMinutos (1 a 60)
     * @return 201 con la tarifa creada; 400 si algun dato es invalido
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TarifaResponse crear(@Valid @RequestBody TarifaRequest req) {
        return service.crear(req);
    }

    /**
     * {@code GET /api/tarifas} - Lista tarifas (historial) ordenadas por tipo y vigencia descendente.
     *
     * @param tipo     (opcional) filtra por CARRO, MOTO o BICICLETA
     * @param vigentes (opcional) {@code true} solo las vigentes; {@code false} solo las historicas
     * @return 200 con la lista; 400 si un filtro tiene un valor invalido
     */
    @GetMapping
    public List<TarifaResponse> listar(@RequestParam(required = false) TipoVehiculo tipo,
                                       @RequestParam(required = false) Boolean vigentes) {
        return service.listar(tipo, vigentes);
    }

    /**
     * {@code GET /api/tarifas/{id}} - Consulta una tarifa por su id (por ejemplo la que indica cobro.idTarifa).
     *
     * @param id identificador de la tarifa (idTarifa)
     * @return 200 con la tarifa; 404 si no existe
     */
    @GetMapping("/{id}")
    public TarifaResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }
}
