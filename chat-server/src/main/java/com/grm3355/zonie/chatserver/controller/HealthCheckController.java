package com.grm3355.zonie.chatserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("chatServerHealthCheckController")
public class HealthCheckController {

	@GetMapping("/health")
	public ResponseEntity<String> healthCheck() {
		// ALB가 200 OK 응답만 받으면 "healthy"로 판단합니다.
		return ResponseEntity.ok("Zonie Chat Server is healthy!");
	}
}
