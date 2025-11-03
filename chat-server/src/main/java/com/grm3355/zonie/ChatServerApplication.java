package com.grm3355.zonie;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatServerApplication {

	public static void main(String[] args) {

		SpringApplication.run(ChatServerApplication.class, args);
		// Tomcat vs Netty 명시
		// -> WebFlux(Reactive)
		// new SpringApplicationBuilder(ChatServerApplication.class)
		// 	.web(WebApplicationType.REACTIVE)
		// 	.run(args);
	}

}
