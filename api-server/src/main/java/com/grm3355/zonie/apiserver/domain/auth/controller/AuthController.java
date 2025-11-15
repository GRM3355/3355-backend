package com.grm3355.zonie.apiserver.domain.auth.controller;

import com.grm3355.zonie.apiserver.domain.auth.dto.RefreshTokenRequest;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.LoginRequest;
import com.grm3355.zonie.apiserver.domain.auth.dto.auth.LoginResponse;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.commonlib.global.enums.ProviderType;
import java.net.URI;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
@Tag(name = "Auth", description = "사용자 토큰 발급")
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
	private final AuthService authService;
	private final RedisTokenService redisTokenService;

	@Operation(summary = "임시 사용자 토큰 발급", description = "위경도 정보를 입력받아 사용자 Access 토큰을 발급합니다.")
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

	//현재는 사용안하므로 주석처리
	// 해당url은 지금은 사용할 일 없지만, 확장성을 위해서 보관한다.
	// 개발할때 업스케일링하는 과정에서나온 url
	// @PostMapping("/oauth2")
	// @Operation(summary = "로그인 (deprecated)", description = "")
	// public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
	// 	LoginResponse response = authService.login(request);
	// 	return ResponseEntity.ok()
	// 			.body(response);
	// }

	@Operation(summary = "카카오 로그인 사용자 토큰 발급", description = "사용자 로그인후 AccessToken, RefreshToken 발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "카카오 로그인 사용자 토큰 발급",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = LoginResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@GetMapping("/kakao/callback")
	public ResponseEntity<LoginResponse> loginWithKakao(@RequestParam("code") String code) {
		LoginResponse response = authService.login(new LoginRequest(ProviderType.KAKAO, code));
		return ResponseEntity.ok()
				.body(response);
	}

	@Operation(summary = "리프레시 토큰 재발급", description = "사용자 토큰 만료시 AccessToken, RefreshToken 재발급합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "리프레시 토큰 재발급",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = LoginResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		LoginResponse authResponse = authService.refreshAccessToken(request.refreshToken());
		return ResponseEntity.ok().body(ApiResponse.success(authResponse));
	}

	@Operation(summary = "로그아웃", description = "서버에 저장된 Refresh 토큰을 삭제하여 로그아웃 처리합니다. 클라이언트 측에서도 저장된 액세스토큰, 리프레시 토큰을 모두 삭제해야 합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "로그아웃 성공",
			content = @Content(mediaType = "application/json"
			))
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
		//200 응답 나오면 프론트엔드에서 액세스토큰, 리프레시 토큰 삭제
		redisTokenService.deleteByToken(request.refreshToken());
		return ResponseEntity.noContent().build();
	}
}
