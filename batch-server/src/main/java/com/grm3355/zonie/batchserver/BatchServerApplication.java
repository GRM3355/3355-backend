package com.grm3355.zonie.batchserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.grm3355.zonie.batchserver", "com.grm3355.zonie.commonlib"})
@EnableJpaRepositories(basePackages = "com.grm3355.zonie.commonlib.domain.festival.repository")
@EntityScan(basePackages = "com.grm3355.zonie.commonlib.domain.festival.entity")
public class BatchServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchServerApplication.class, args);
	}

}
