package com.wag.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Ponto de entrada da aplicação Spring Boot Web Autogestão. */
@SpringBootApplication
public class ApiApplication {

	/** Inicializa o contexto Spring e inicia o servidor web da aplicação. */
	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}

}
