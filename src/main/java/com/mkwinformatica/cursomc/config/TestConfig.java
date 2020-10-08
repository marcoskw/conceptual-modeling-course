package com.mkwinformatica.cursomc.config;

import java.text.ParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.mkwinformatica.cursomc.services.DBService;

@Configuration
@Profile("test")
public class TestConfig {

	@Autowired
	private DBService dbServoce;
	
	@Bean
	public boolean intantiateDatabase() throws ParseException {
		dbServoce.instantiateTestDatabase();
		return true;
	}
}
