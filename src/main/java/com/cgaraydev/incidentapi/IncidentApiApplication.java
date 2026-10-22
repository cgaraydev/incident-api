package com.cgaraydev.incidentapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@ConfigurationPropertiesScan
public class IncidentApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(IncidentApiApplication.class, args);
	}

}
