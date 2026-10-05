package com.sena.parkingapi.dto;

public record CobroDetalle(long minutosTotales, long horasCompletas, long fraccionesCobradas,
                           long tarifaHora, long tarifaFraccion, int fraccionMinutos, long valorTotal) {
}
