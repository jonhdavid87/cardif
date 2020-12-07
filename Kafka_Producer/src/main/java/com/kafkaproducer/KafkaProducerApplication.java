package com.kafkaproducer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.core.KafkaTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kafkaproducer.entity.EmissionData;

@SpringBootApplication
public class KafkaProducerApplication implements ApplicationRunner {

	private ObjectMapper mapper = new ObjectMapper();
	private static final Logger logger = LoggerFactory.getLogger(KafkaProducerApplication.class);
	
	@Autowired
	private KafkaTemplate<Integer, String> kafkaTemplate;
	
	public static void main(String[] args) {
		SpringApplication.run(KafkaProducerApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {
		EmissionData emissionData = new EmissionData();
		emissionData.setCertificado("123455");
		emissionData.setApellido("aponte navarrete");
		emissionData.setEdad("33");
		emissionData.setNombre("Jonathan");
		emissionData.setPeriodicidad("anual");
		emissionData.setPlan("1");
		emissionData.setProducto("1332");
		
		try {
			kafkaTemplate.send("emission-topic", mapper.writeValueAsString(emissionData));
			logger.info("Mensaje enviado");
		} catch (JsonProcessingException e) {
			e.printStackTrace();
		}
		
	}

}
