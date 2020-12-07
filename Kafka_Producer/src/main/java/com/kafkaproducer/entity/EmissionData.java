package com.kafkaproducer.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class EmissionData {
	private String certificado;
	private String nombre;
	private String apellido;
	private String edad;
	private String producto;
	private String plan;
	private String periodicidad;
	
	
}
