package com.grm3355.zonie.batchserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication // 기본적으로 com.grm3355.zonie.batchserver 스캔
@EnableScheduling  // 스케줄링 활성화
@EnableJpaRepositories(basePackages = {
	"com.grm3355.zonie.commonlib"
})
@EntityScan(basePackages = {
	"com.grm3355.zonie.commonlib"
})
@ComponentScan(basePackages = {
	"com.grm3355.zonie.batchserver", // SpringBootApplication과 중복 but, 명시적으로 자기 자신을 포함
	"com.grm3355.zonie.commonlib"
})
public class BatchServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchServerApplication.class, args);
	}

}
