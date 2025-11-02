package com.grm3355.zonie.batchserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
	"com.grm3355.zonie.batchserver",
	"com.grm3355.zonie.commonlib"
})
@EnableJpaRepositories(basePackages = {
	"com.grm3355.zonie.commonlib.domain.festival.repository",
	"com.grm3355.zonie.commonlib.domain.batch.repository",
})
@EntityScan(basePackages = {
	"com.grm3355.zonie.commonlib.domain.festival.entity",
	"com.grm3355.zonie.commonlib.domain.batch.entity"
})
@EnableScheduling  // 스케줄링 활성화
public class BatchServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchServerApplication.class, args);
	}

}
