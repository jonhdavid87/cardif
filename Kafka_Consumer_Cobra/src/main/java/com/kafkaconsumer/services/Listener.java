package com.kafkaconsumer.services;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.kafkaconsumer.entity.EmissionData;

@Component
public class Listener {
	private static final Logger log = LoggerFactory.getLogger(Listener.class);
	
	@KafkaListener(topics = "emission-topic", groupId = "cardifGroup")
	public void listen(String message) {
		log.info("Message received {}", message);
		
		EmissionData emissionData = new Gson().fromJson(message, EmissionData.class);
		
		emissionData = null;
		
	}
}