package com.grm3355.zonie.batchserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 1. common-lib의 @Service, @Component 등을 스캔하기 위해
 * 최상위 패키지인 "com.grm3355.zonie"를 스캔합니다.
 */
@SpringBootApplication(
	scanBasePackages = "com.grm3355.zonie",
	exclude = {MongoAutoConfiguration.class, MongoDataAutoConfiguration.class}
)
@EnableScheduling
/**
 * 2. JPA(PostgreSQL) 리포지토리, 엔티티 패키지들만 지정합니다.
 */
@EnableJpaRepositories(basePackages = {
	"com.grm3355.zonie.commonlib.domain.chatroom.repository",
	"com.grm3355.zonie.commonlib.domain.festival.repository",
	"com.grm3355.zonie.commonlib.domain.user.repository"
})
@EntityScan(basePackages = {
	"com.grm3355.zonie.commonlib.domain.chatroom.entity",
	"com.grm3355.zonie.commonlib.domain.festival.entity",
	"com.grm3355.zonie.commonlib.domain.user.entity"
})
public class BatchServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BatchServerApplication.class, args);
	}
}