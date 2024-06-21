package com.fredy.mobiAd;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling
public class MobiAdApplication {

	public static void main(String[] args) {
		SpringApplication.run(MobiAdApplication.class, args);
	}

}
