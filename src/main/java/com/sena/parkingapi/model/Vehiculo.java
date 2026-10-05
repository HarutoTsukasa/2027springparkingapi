package com.sena.parkingapi.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehiculo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id_vehiculo;

	private String placa;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private TipoVehiculo tipo;

	private String descripcion;

	@OneToMany(mappedBy = "vehiculo")
	private List<RegistroParqueo> registros;
}
