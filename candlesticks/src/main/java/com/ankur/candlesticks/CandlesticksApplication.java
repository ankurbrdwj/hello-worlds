package com.ankur.candlesticks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@EnableJpaRepositories
@SpringBootApplication
public class CandlesticksApplication {

	public static void main(String[] args) {
		SpringApplication.run(CandlesticksApplication.class, args);
	}

}
