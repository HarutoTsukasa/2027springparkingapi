package com.sena.parkingapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Tarifa por tipo de vehiculo. Es un historial: nunca se edita, se crea una nueva
 * y la anterior se cierra (vigenteHasta). La tarifa vigente tiene vigenteHasta = null.
 */
@Entity
@Table(name = "tarifas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tarifa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id_tarifa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoVehiculo tipo;

    @Column(nullable = false)
    private Long valorHora;

    @Column(nullable = false)
    private Long valorFraccion;

    @Column(nullable = false)
    private Integer fraccionMinutos;

    @Column(nullable = false)
    private LocalDateTime vigenteDesde;

    private LocalDateTime vigenteHasta;
}
