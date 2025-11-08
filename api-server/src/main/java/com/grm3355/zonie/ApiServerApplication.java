package com.grm3355.zonie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication(scanBasePackages = "com.grm3355.zonie")

@EnableJpaRepositories(basePackages = {
	"com.grm3355.zonie.commonlib.domain.chatroom.repository",
	"com.grm3355.zonie.commonlib.domain.festival.repository",
	"com.grm3355.zonie.commonlib.domain.user.repository"
})
@EnableMongoRepositories(basePackages = "com.grm3355.zonie.commonlib.domain.message.repository")
@EntityScan(basePackages = {
	"com.grm3355.zonie.commonlib.domain.chatroom.entity",
	"com.grm3355.zonie.commonlib.domain.festival.entity",
	"com.grm3355.zonie.commonlib.domain.user.entity"
})
public class ApiServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(ApiServerApplication.class, args);
	}

}
