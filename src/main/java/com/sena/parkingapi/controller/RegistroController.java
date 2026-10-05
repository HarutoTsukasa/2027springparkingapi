package com.sena.parkingapi.controller;

import com.sena.parkingapi.dto.EntradaRequest;
import com.sena.parkingapi.dto.RegistroResponse;
import com.sena.parkingapi.service.RegistroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Entradas, salidas y cobro del parqueadero.
 * Base: {@code /api/registros}. Un registro esta "activo" mientras no tenga fecha de salida.
 * La salida y el cobro se pueden hacer por id de registro o por placa (registro activo de la placa).
 */
@RestController
@RequestMapping("/api/registros")
@RequiredArgsConstructor
public class RegistroController {

    private final RegistroService service;

    /**
     * {@code POST /api/registros/entrada} - Registra la entrada de un vehiculo y le asigna un espacio.
     * Si no se envia numeroEspacio se asigna el espacio LIBRE de menor numero del mismo tipo.
     * Si la placa no existe se crea el vehiculo; si existe, debe ser del mismo tipo.
     *
     * @param req placa (obligatoria, 5 a 7 alfanumericos), tipo, descripcion (opcional) y numeroEspacio (opcional)
     * @return 201 con el registro; 400 datos invalidos; 404 espacio inexistente;
     *         409 placa ya dentro / placa de otro tipo / espacio ocupado o de otro tipo / sin espacios libres / sin tarifa vigente
     */
    @PostMapping("/entrada")
    @ResponseStatus(HttpStatus.CREATED)
    public RegistroResponse entrada(@Valid @RequestBody EntradaRequest req) {
        return service.registrarEntrada(req);
    }

    // ----- por id de registro -----

    /**
     * {@code POST /api/registros/{id}/salida} - Registra la salida del registro indicado:
     * fija la fecha de salida, calcula y guarda el valor pagado y libera el espacio.
     *
     * @param id identificador del registro (idRegistro)
     * @return 200 con el registro cerrado y el detalle del cobro; 404 si no existe; 409 si ya tiene salida
     */
    @PostMapping("/{id}/salida")
    public RegistroResponse salida(@PathVariable Long id) {
        return service.registrarSalida(id);
    }

    /**
     * {@code GET /api/registros/{id}/cobro} - Calcula cuanto costaria salir ahora, sin cerrar el registro.
     * Si el registro ya tiene salida devuelve el cobro final.
     *
     * @param id identificador del registro
     * @return 200 con el registro y el detalle del cobro; 404 si no existe
     */
    @GetMapping("/{id}/cobro")
    public RegistroResponse cobro(@PathVariable Long id) {
        return service.consultarCobro(id);
    }

    /**
     * {@code GET /api/registros/{id}} - Consulta un registro por su id (activo o finalizado).
     *
     * @param id identificador del registro
     * @return 200 con el registro; 404 si no existe
     */
    @GetMapping("/{id}")
    public RegistroResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    // ----- por placa (registro activo) -----

    /**
     * {@code POST /api/registros/placa/{placa}/salida} - Registra la salida del vehiculo que esta
     * dentro con esa placa (mismo efecto que la salida por id).
     *
     * @param placa placa del vehiculo; se normaliza (mayusculas, sin guiones ni espacios)
     * @return 200 con el registro cerrado y el detalle del cobro; 400 si la placa es invalida;
     *         404 si la placa no tiene un registro activo (tambien al repetir la salida)
     */
    @PostMapping("/placa/{placa}/salida")
    public RegistroResponse salidaPorPlaca(@PathVariable String placa) {
        return service.registrarSalidaPorPlaca(placa);
    }

    /**
     * {@code GET /api/registros/placa/{placa}/cobro} - Calcula cuanto costaria salir ahora
     * al vehiculo con esa placa, sin cerrar el registro.
     *
     * @param placa placa del vehiculo
     * @return 200 con el registro activo y el detalle del cobro; 400 placa invalida; 404 sin registro activo
     */
    @GetMapping("/placa/{placa}/cobro")
    public RegistroResponse cobroPorPlaca(@PathVariable String placa) {
        return service.consultarCobroPorPlaca(placa);
    }

    /**
     * {@code GET /api/registros/placa/{placa}} - Consulta el registro activo (sin salida) de una placa.
     *
     * @param placa placa del vehiculo
     * @return 200 con el registro; 400 placa invalida; 404 sin registro activo
     */
    @GetMapping("/placa/{placa}")
    public RegistroResponse activoPorPlaca(@PathVariable String placa) {
        return service.obtenerActivoPorPlaca(placa);
    }

    /**
     * {@code GET /api/registros} - Lista registros, del mas reciente al mas antiguo.
     *
     * @param activos (opcional) {@code true} solo los que estan dentro; {@code false} solo los finalizados;
     *                sin parametro, todos
     * @return 200 con la lista (vacia si no hay coincidencias)
     */
    @GetMapping
    public List<RegistroResponse> listar(@RequestParam(required = false) Boolean activos) {
        return service.listar(activos);
    }
}
