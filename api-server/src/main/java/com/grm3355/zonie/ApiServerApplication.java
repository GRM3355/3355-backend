package com.grm3355.zonie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.grm3355.zonie")
public class ApiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiServerApplication.class, args);
	}

}
