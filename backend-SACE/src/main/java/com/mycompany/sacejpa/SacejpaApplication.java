package com.mycompany.sacejpa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SacejpaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SacejpaApplication.class, args);
		System.out.println("La aplicacion SaceJPA esta iniciada correctamente");
	}
}