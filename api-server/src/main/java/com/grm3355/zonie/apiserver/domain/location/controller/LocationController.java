package com.grm3355.zonie.apiserver.domain.location.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.location.service.LocationService;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "location", description = "사용자 위치정보 갱신 및 확인")
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

	private final RedisTokenService redisTokenService;
	private final LocationService locationService;

	@Operation(summary = "토큰 정보 업데이트", description = "위도, 경도 정보를 받아서 Redis에서 수정한다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업데이트 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
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
	@PutMapping("/update")
	public ResponseEntity<?> updateLocation(@AuthenticationPrincipal UserDetailsImpl user,
		@Valid @RequestBody LocationDto locationDto) {
		//10분 단위로 호출함.
		boolean response = redisTokenService.updateLocationInfo(locationDto, user.getUsername());
		return ResponseEntity.ok(ApiResponse.success(response));

	}

	@Operation(summary = "축제 영역 체크", description = "위도, 경도 정보를 받아서 Redis에서 수정한다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
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
	@GetMapping("/festivalVerify")
	public ResponseEntity<?> getfestivalVerify(String token, String festivalId) {
		boolean response = locationService.getFestivalVerify(token, festivalId);
		return ResponseEntity.ok(ApiResponse.success(response));

	}

	@Operation(summary = "채팅방 영역 체크", description = "위도, 경도 정보를 받아서 Redis에서 수정한다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
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

	@GetMapping("/chatroomVerify")
	public ResponseEntity<?> getchatroomVerify(String token, String chatroomId) {
		boolean response = locationService.getChatroomVerify(token, chatroomId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
