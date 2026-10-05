package com.sena.parkingapi.service;

import com.sena.parkingapi.config.ZonaHoraria;
import com.sena.parkingapi.dto.CobroDetalle;
import com.sena.parkingapi.dto.EntradaRequest;
import com.sena.parkingapi.dto.RegistroResponse;
import com.sena.parkingapi.dto.VehiculoResponse;
import com.sena.parkingapi.exception.ApiException;
import com.sena.parkingapi.model.*;
import com.sena.parkingapi.repository.EspacioParqueoRepository;
import com.sena.parkingapi.repository.RegistroParqueoRepository;
import com.sena.parkingapi.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final RegistroParqueoRepository registroRepo;
    private final EspacioParqueoRepository espacioRepo;
    private final VehiculoRepository vehiculoRepo;
    private final TarifaService tarifaService;

    @Transactional
    public RegistroResponse registrarEntrada(EntradaRequest req) {
        TipoVehiculo tipo = req.tipo();
        String placa = normalizarPlaca(req.placa());
        if (placa == null) {
            throw ApiException.badRequest("La placa es obligatoria");
        }

        // El bloqueo del vehiculo va primero: serializa entradas simultaneas de la misma placa,
        // de modo que la verificacion de "ya esta dentro" vea siempre datos confirmados.
        Vehiculo vehiculo = vehiculoRepo.bloquearPorPlaca(placa).orElse(null);

        if (registroRepo.existsByVehiculoPlacaAndFechaSalidaIsNull(placa)) {
            throw ApiException.conflict("El vehiculo con placa " + placa + " ya esta dentro del parqueadero");
        }
        if (vehiculo != null && vehiculo.getTipo() != tipo) {
            throw ApiException.conflict("La placa " + placa + " esta registrada como " + vehiculo.getTipo());
        }
        tarifaService.exigirTarifaVigente(tipo);

        if (vehiculo == null) {
            vehiculo = Vehiculo.builder().placa(placa).tipo(tipo).build();
        }
        if (req.descripcion() != null) {
            vehiculo.setDescripcion(req.descripcion());
        }

        EspacioParqueo espacio = elegirEspacio(req.numeroEspacio(), tipo);
        vehiculo = vehiculoRepo.save(vehiculo);
        espacio.setEstado(EstadoEspacio.OCUPADO);

        RegistroParqueo registro = registroRepo.save(RegistroParqueo.builder()
                .vehiculo(vehiculo).espacio(espacio).fechaEntrada(ZonaHoraria.ahora()).build());
        return RegistroResponse.de(registro, null);
    }

    // ---------- salida ----------

    @Transactional
    public RegistroResponse registrarSalida(Long id) {
        RegistroParqueo r = registroRepo.bloquearPorId(id)
                .orElseThrow(() -> ApiException.notFound("Registro " + id + " no encontrado"));
        if (r.getFechaSalida() != null) {
            throw ApiException.conflict("El registro " + id + " ya tiene salida registrada");
        }
        return cerrar(r);
    }

    @Transactional
    public RegistroResponse registrarSalidaPorPlaca(String placa) {
        String p = placaObligatoria(placa);
        RegistroParqueo r = registroRepo.bloquearActivoPorPlaca(p)
                .orElseThrow(() -> sinRegistroActivo(p));
        return cerrar(r);
    }

    private RegistroResponse cerrar(RegistroParqueo r) {
        LocalDateTime salida = ZonaHoraria.ahora();
        CobroDetalle cobro = tarifaService.calcular(r.getVehiculo().getTipo(), r.getFechaEntrada(), salida);

        r.setFechaSalida(salida);
        r.setValorPagado((double) cobro.valorTotal());
        r.getEspacio().setEstado(EstadoEspacio.LIBRE);
        return RegistroResponse.de(r, cobro);
    }

    // ---------- consultas ----------

    /** Cuanto costaria salir ahora, sin cerrar el registro. Si ya cerro, devuelve el cobro final. */
    @Transactional(readOnly = true)
    public RegistroResponse consultarCobro(Long id) {
        return conCobro(buscar(id));
    }

    /** Igual que consultarCobro, pero sobre el registro activo de una placa. */
    @Transactional(readOnly = true)
    public RegistroResponse consultarCobroPorPlaca(String placa) {
        String p = placaObligatoria(placa);
        return conCobro(registroRepo.findByVehiculoPlacaAndFechaSalidaIsNull(p)
                .orElseThrow(() -> sinRegistroActivo(p)));
    }

    @Transactional(readOnly = true)
    public RegistroResponse obtener(Long id) {
        return RegistroResponse.de(buscar(id), null);
    }

    @Transactional(readOnly = true)
    public RegistroResponse obtenerActivoPorPlaca(String placa) {
        String p = placaObligatoria(placa);
        return RegistroResponse.de(registroRepo.findByVehiculoPlacaAndFechaSalidaIsNull(p)
                .orElseThrow(() -> sinRegistroActivo(p)), null);
    }

    @Transactional(readOnly = true)
    public List<RegistroResponse> listar(Boolean activos) {
        List<RegistroParqueo> lista;
        if (activos == null) {
            lista = registroRepo.findAllByOrderByFechaEntradaDesc();
        } else if (activos) {
            lista = registroRepo.findByFechaSalidaIsNullOrderByFechaEntradaDesc();
        } else {
            lista = registroRepo.findByFechaSalidaIsNotNullOrderByFechaEntradaDesc();
        }
        return lista.stream().map(r -> RegistroResponse.de(r, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<RegistroResponse> historialPorPlaca(String placa) {
        String p = placaObligatoria(placa);
        return registroRepo.findByVehiculoPlacaOrderByFechaEntradaDesc(p).stream()
                .map(r -> RegistroResponse.de(r, null)).toList();
    }

    @Transactional(readOnly = true)
    public List<VehiculoResponse> listarVehiculos() {
        return vehiculoRepo.findAll().stream().map(VehiculoResponse::de).toList();
    }

    // ---------- helpers ----------

    private RegistroResponse conCobro(RegistroParqueo r) {
        LocalDateTime hasta = r.getFechaSalida() != null ? r.getFechaSalida() : ZonaHoraria.ahora();
        CobroDetalle c = tarifaService.calcular(r.getVehiculo().getTipo(), r.getFechaEntrada(), hasta);
        return RegistroResponse.de(r, c);
    }

    private EspacioParqueo elegirEspacio(Integer numero, TipoVehiculo tipo) {
        if (numero != null) {
            EspacioParqueo e = espacioRepo.bloquearPorNumero(numero)
                    .orElseThrow(() -> ApiException.notFound("Espacio " + numero + " no existe"));
            if (e.getTipo() != tipo) {
                throw ApiException.conflict("El espacio " + numero + " es para " + e.getTipo() + ", no para " + tipo);
            }
            if (e.getEstado() == EstadoEspacio.OCUPADO) {
                throw ApiException.conflict("El espacio " + numero + " esta ocupado");
            }
            return e;
        }
        return espacioRepo.findFirstByTipoAndEstadoOrderByNumeroAsc(tipo, EstadoEspacio.LIBRE)
                .orElseThrow(() -> ApiException.conflict("No hay espacios libres para " + tipo));
    }

    private RegistroParqueo buscar(Long id) {
        return registroRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Registro " + id + " no encontrado"));
    }

    private ApiException sinRegistroActivo(String placa) {
        return ApiException.notFound("El vehiculo con placa " + placa + " no tiene un registro activo");
    }

    private String placaObligatoria(String placa) {
        String p = normalizarPlaca(placa);
        if (p == null) {
            throw ApiException.badRequest("Placa invalida");
        }
        return p;
    }

    private String normalizarPlaca(String placa) {
        if (placa == null || placa.isBlank()) {
            return null;
        }
        String p = placa.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        if (!p.matches("[A-Z0-9]{5,7}")) {
            throw ApiException.badRequest("Placa invalida: debe tener entre 5 y 7 caracteres alfanumericos");
        }
        return p;
    }
}
