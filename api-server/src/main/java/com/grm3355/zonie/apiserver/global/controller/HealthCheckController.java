package com.grm3355.zonie.apiserver.global.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Hidden;

@Hidden
@RestController
public class HealthCheckController {

	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		// ALB가 200 OK 응답만 받으면 "healthy"로 판단합니다.
		return ResponseEntity.ok("Zonie Api Server is healthy!~~~~~!!");
	}

	@Hidden
	@RestController
	public class RootController {

		@GetMapping("/")
		public ResponseEntity<String> rootHealth() {
			return ResponseEntity.ok("");
		}
	}
}
