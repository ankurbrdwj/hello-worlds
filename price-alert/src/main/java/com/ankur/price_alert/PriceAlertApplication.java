package com.ankur.price_alert;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PriceAlertApplication {

	public static void main(String[] args) {
		SpringApplication.run(PriceAlertApplication.class, args);
	}

}
