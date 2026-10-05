package com.sena.parkingapi.service;

import com.sena.parkingapi.config.ZonaHoraria;
import com.sena.parkingapi.dto.CobroDetalle;
import com.sena.parkingapi.dto.TarifaRequest;
import com.sena.parkingapi.dto.TarifaResponse;
import com.sena.parkingapi.exception.ApiException;
import com.sena.parkingapi.model.Tarifa;
import com.sena.parkingapi.model.TipoVehiculo;
import com.sena.parkingapi.repository.TarifaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Tarifa por hora y fraccion (se usa la tarifa vigente en el momento de la salida):
 *  - Las horas completas se cobran a tarifa de hora.
 *  - El tiempo sobrante se cobra por fracciones iniciadas (bloques de N minutos),
 *    sin superar nunca el valor de una hora.
 *  - Los segundos se redondean hacia arriba a minuto; el minimo cobrable es una fraccion.
 */
@Service
@RequiredArgsConstructor
public class TarifaService {

    private final TarifaRepository tarifaRepo;

    /** Crea una nueva tarifa vigente para el tipo y cierra la anterior. */
    @Transactional
    public TarifaResponse crear(TarifaRequest req) {
        LocalDateTime ahora = ZonaHoraria.ahora();
        tarifaRepo.bloquearVigente(req.tipo()).ifPresent(t -> t.setVigenteHasta(ahora));
        Tarifa nueva = tarifaRepo.save(Tarifa.builder()
                .tipo(req.tipo())
                .valorHora(req.valorHora())
                .valorFraccion(req.valorFraccion())
                .fraccionMinutos(req.fraccionMinutos())
                .vigenteDesde(ahora)
                .build());
        return TarifaResponse.de(nueva);
    }

    @Transactional(readOnly = true)
    public List<TarifaResponse> listar(TipoVehiculo tipo, Boolean vigentes) {
        return tarifaRepo.findAllByOrderByTipoAscVigenteDesdeDesc().stream()
                .filter(t -> tipo == null || t.getTipo() == tipo)
                .filter(t -> vigentes == null || (t.getVigenteHasta() == null) == vigentes)
                .map(TarifaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TarifaResponse obtener(Long id) {
        return TarifaResponse.de(tarifaRepo.findById(id)
                .orElseThrow(() -> ApiException.notFound("Tarifa " + id + " no encontrada")));
    }

    /** Falla si el tipo no tiene tarifa vigente (se usa al registrar la entrada). */
    @Transactional(readOnly = true)
    public void exigirTarifaVigente(TipoVehiculo tipo) {
        if (!tarifaRepo.existsByTipoAndVigenteHastaIsNull(tipo)) {
            throw ApiException.conflict("No hay tarifa vigente para " + tipo);
        }
    }

    @Transactional(readOnly = true)
    public CobroDetalle calcular(TipoVehiculo tipo, LocalDateTime entrada, LocalDateTime salida) {
        Tarifa t = tarifaVigenteEn(tipo, salida);
        int fm = t.getFraccionMinutos();

        long segundos = Math.max(0, Duration.between(entrada, salida).getSeconds());
        long minutos = (segundos + 59) / 60;

        long horas = minutos / 60;
        long resto = minutos % 60;
        long fracciones = (resto + fm - 1) / fm;
        if (horas == 0 && fracciones == 0) {
            fracciones = 1;
        }

        long cobroFracciones = Math.min(fracciones * t.getValorFraccion(), t.getValorHora());
        long total = horas * t.getValorHora() + cobroFracciones;

        return new CobroDetalle(t.getId_tarifa(), minutos, horas, fracciones,
                t.getValorHora(), t.getValorFraccion(), fm, total);
    }

    private Tarifa tarifaVigenteEn(TipoVehiculo tipo, LocalDateTime momento) {
        return tarifaRepo.vigentesEn(tipo, momento).stream().findFirst()
                .or(() -> tarifaRepo.findFirstByTipoAndVigenteHastaIsNull(tipo))
                .orElseThrow(() -> ApiException.conflict("No hay tarifa vigente para " + tipo));
    }
}
