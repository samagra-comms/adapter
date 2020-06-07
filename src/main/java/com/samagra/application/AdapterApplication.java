package com.samagra.application;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@ComponentScan(basePackages = {"com.samagra.*"})
public class AdapterApplication {
	public static void main(String[] args) {
		SpringApplication.run(AdapterApplication.class, args);
	}
}
