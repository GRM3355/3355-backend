package com.grm3355.zonie.apiserver.domain.auth.controller;

import java.net.URI;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.auth.dto.AuthResponse;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.service.AuthService;
import com.grm3355.zonie.apiserver.global.swagger.ApiError400;
import com.grm3355.zonie.apiserver.global.swagger.ApiError405;
import com.grm3355.zonie.apiserver.global.swagger.ApiError415;
import com.grm3355.zonie.apiserver.global.swagger.ApiError429;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Auth & User", description = "사용자 토큰 발급")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@Operation(summary = "사용자 토큰 발급", description = "위경도 정보를 입력받아 사용자 Access 토큰을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "201",
			description = "토큰 발급 성공 (신규 생성)",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = AuthResponse.class),
				examples = @ExampleObject(
					name = "CREATED",
					value = "{\"success\":true,\"data\":{\"accessToken\":\"...\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@PostMapping("/tokens")
	public ResponseEntity<?> register(@Valid @RequestBody LocationDto locationDto, HttpServletRequest request) {
		String path = request != null ? request.getRequestURI() : null;
		URI location = URI.create(Objects.requireNonNull(path));

		// 토큰이 없으면 register 처리
		AuthResponse response2 = authService.register(locationDto);
		return ResponseEntity.created(location).body(ApiResponse.success(response2));
	}
}
