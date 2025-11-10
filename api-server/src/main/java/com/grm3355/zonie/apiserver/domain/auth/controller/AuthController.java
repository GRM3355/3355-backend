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

	@Operation(summary = "사용자 토큰 발급", description = "위도, 경도 입력받아, Access 토큰을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "토큰 발급성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"data\":{\"accessToken\":\"...\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "입력값 유효성 검증 실패",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "BAD_REQUEST",
					value = "{\"success\":false,\"status\":400,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "405",
			description = "허용되지 않은 메소드",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "METHOD_NOT_ALLOWED",
					value = "{\"success\":false,\"status\":405,\"error\":{\"code\":\"METHOD_NOT_ALLOWED\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "415",
			description = "UNSUPPORTED_MEDIA_TYPE",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "UNSUPPORTED_MEDIA_TYPE",
					value = "{\"success\":false,\"status\":415,\"error\":{\"code\":\"UNSUPPORTED_MEDIA_TYPE\",\"message\":\"잘못된 콘텐츠 타입입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "429",
			description = "요청 횟수 초과",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "TOO_MANY_REQUESTS",
					value = "{\"success\":false,\"status\":429,\"error\":{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
				)
			)
		)
	})
	@PostMapping("/token-register")
	public ResponseEntity<?> register(@Valid @RequestBody LocationDto locationDto, HttpServletRequest request) {
		String path = request != null ? request.getRequestURI() : null;
		URI location = URI.create(Objects.requireNonNull(path));

		// 토큰이 없으면 register 처리
		AuthResponse response2 = authService.register(locationDto);
		return ResponseEntity.created(location).body(ApiResponse.success(response2));
	}
}
