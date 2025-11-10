package com.grm3355.zonie.apiserver.global.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
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
public class DataAccessConfig {
	// 설정 어노테이션만 담고, 내부는 비워둠.
}
