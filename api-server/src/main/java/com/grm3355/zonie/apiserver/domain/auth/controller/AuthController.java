package com.grm3355.zonie.apiserver.domain.auth.controller;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
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
@Tag(name = "Auth & User", description = "사용자 인증")
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;

	@Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인을 처리하고, Access/Refresh 토큰을 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "OK",
				value = "{\"success\":true,\"status\":200,\"message\":\"OK\",\"data\":{\"grantType\":\"Bearer\",\"accessToken\":\"...\",\"refreshToken\":\"...\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "VALIDATION_FAILED",
				value = "{\"success\":false,\"status\":400,\"message\":\"VALIDATION_FAILED\",\"data\":{\"code\":\"VALIDATION_FAILED\",\"message\":\"요청 본문 검증 실패\",\"traceId\":null,\"path\":\"/api/v1/auth/login\",\"errors\":{\"password\":\"비밀번호는 필수 입력 값입니다.\"}},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "TOO_MANY_REQUESTS",
				value = "{\"success\":false,\"status\":429,\"message\":\"요청 횟수가 너무 많습니다. 잠시 후 다시 시도해주세요.\",\"data\":null,\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
			)
		))
	})
	@PostMapping("/token")
	public ResponseEntity<?> login(@AuthenticationPrincipal UserDetailsImpl user,
		@Valid @RequestBody LocationDto locationDto, HttpServletRequest request) {
		System.out.println("====> login start 1234");
		String path = request != null ? request.getRequestURI() : null;

		//토큰 정보 체크
		if (user != null && user.getUsername() != null && !user.getUsername().isBlank()) {
			// 이미 토큰이 존재하면 login 처리
			boolean response = authService.login(locationDto, user.getUsername());
			return ResponseEntity.ok(ApiResponse.success(response));
		} else {
			// 토큰이 없으면 register 처리
			AuthResponse response2 = authService.register(locationDto, request);
			URI location = URI.create(path);
			return ResponseEntity.created(location).body(ApiResponse.success(response2));
		}

	}

}
