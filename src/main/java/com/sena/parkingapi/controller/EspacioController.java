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

/**
 * Administracion de los espacios del parqueadero.
 * Base: {@code /api/espacios}. Al arrancar con la tabla vacia se crean 50 espacios
 * (25 carros, 20 motos, 5 bicicletas); ver {@code parking.espacios.iniciales.*}.
 */
@RestController
@RequestMapping("/api/espacios")
@RequiredArgsConstructor
public class EspacioController {

    private final EspacioService service;

    /**
     * {@code POST /api/espacios} - Crea un espacio (estado inicial LIBRE).
     *
     * @param req numero (entero positivo, unico) y tipo (CARRO, MOTO o BICICLETA)
     * @return 201 con el espacio creado; 400 si faltan datos; 409 si el numero ya existe
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EspacioResponse crear(@Valid @RequestBody EspacioRequest req) {
        return service.crear(req);
    }

    /**
     * {@code POST /api/espacios/lote} - Crea varios espacios consecutivos del mismo tipo
     * (maximo 500). Es todo o nada: si algun numero existe no se crea ninguno.
     *
     * @param req desde, hasta (inclusive) y tipo
     * @return 201 con la lista creada; 400 si desde &gt; hasta o el lote excede 500; 409 si algun numero existe
     */
    @PostMapping("/lote")
    @ResponseStatus(HttpStatus.CREATED)
    public List<EspacioResponse> crearLote(@Valid @RequestBody EspacioLoteRequest req) {
        return service.crearLote(req);
    }

    /**
     * {@code GET /api/espacios} - Lista los espacios ordenados por numero.
     *
     * @param tipo   (opcional) filtra por CARRO, MOTO o BICICLETA
     * @param estado (opcional) filtra por LIBRE u OCUPADO
     * @return 200 con la lista (vacia si no hay coincidencias); 400 si un filtro tiene un valor invalido
     */
    @GetMapping
    public List<EspacioResponse> listar(@RequestParam(required = false) TipoVehiculo tipo,
                                        @RequestParam(required = false) EstadoEspacio estado) {
        return service.listar(tipo, estado);
    }

    /**
     * {@code GET /api/espacios/{id}} - Consulta un espacio por su id.
     *
     * @param id identificador del espacio (idEspacio)
     * @return 200 con el espacio; 404 si no existe
     */
    @GetMapping("/{id}")
    public EspacioResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    /**
     * {@code PUT /api/espacios/{id}} - Modifica numero y tipo de un espacio.
     * No se permite si el espacio esta OCUPADO.
     *
     * @param id  identificador del espacio
     * @param req nuevo numero y tipo
     * @return 200 con el espacio actualizado; 404 si no existe; 409 si esta ocupado o el numero ya existe
     */
    @PutMapping("/{id}")
    public EspacioResponse actualizar(@PathVariable Long id, @Valid @RequestBody EspacioRequest req) {
        return service.actualizar(id, req);
    }

    /**
     * {@code DELETE /api/espacios/{id}} - Elimina un espacio. No se permite si esta OCUPADO
     * ni si ya tiene historial de registros.
     *
     * @param id identificador del espacio
     * @return 204 sin cuerpo; 404 si no existe; 409 si esta ocupado o tiene historial
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id) {
        service.eliminar(id);
    }
}
