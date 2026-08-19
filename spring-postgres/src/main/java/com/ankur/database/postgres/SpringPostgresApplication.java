package com.ankur.database.postgres;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class SpringPostgresApplication {

	public static void main(String[] args) {
		SpringApplication.run(SpringPostgresApplication.class, args);
	}

}
