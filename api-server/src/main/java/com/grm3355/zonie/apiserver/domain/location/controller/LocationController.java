package com.grm3355.zonie.apiserver.domain.location.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.auth.service.RedisTokenService;
import com.grm3355.zonie.apiserver.domain.location.dto.ChatRoomZoneVarifyResponse;
import com.grm3355.zonie.apiserver.domain.location.dto.FestivalZoneVarifyResponse;
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
@Tag(name = "위치토큰정보", description = "위치토큰정보 갱신 및 확인")
@RequestMapping("/api/v1/location")
@RequiredArgsConstructor
public class LocationController {

	private final RedisTokenService redisTokenService;
	private final LocationService locationService;

	@Operation(summary = "location 토큰의 위치정보 업데이트", description = "위도, 경도, accessToken을 받아서 Redis에서 정보를 수정한다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 발급성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "OK",
				value = "{\"success\":true,\"data\":{\"message\":\"갱신되었습니다.\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "BAD_REQUEST",
				value = "{\"success\":false,\"status\":400,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", description = "허용되지 않은 메소드", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "METHOD_NOT_ALLOWED",
				value = "{\"success\":false,\"status\":405,\"error\":{\"code\":\"METHOD_NOT_ALLOWED\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "UNSUPPORTED_MEDIA_TYPE", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "UNSUPPORTED_MEDIA_TYPE",
				value = "{\"success\":false,\"status\":415,\"error\":{\"code\":\"UNSUPPORTED_MEDIA_TYPE\",\"message\":\"잘못된 콘텐츠 타입입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "TOO_MANY_REQUESTS",
				value = "{\"success\":false,\"status\":429,\"error\":{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
			)
		))
	})
	@PutMapping("/update")
	public ResponseEntity<?> updateLocation(@AuthenticationPrincipal UserDetailsImpl userDetails,
		@Valid @RequestBody LocationDto locationDto) {
		//10분 단위로 호출함.
		LocationTokenResponse response = locationService.update(locationDto, userDetails);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	@Operation(summary = "축제 영역 체크", description = "위도, 경도 정보를 받아서 기존의 Redis값과 비교한다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 발급성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "OK",
				value = "{\"success\":true,\"data\":{\"message\":\"갱신되었습니다.\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 유효성 검증 실패", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "BAD_REQUEST",
				value = "{\"success\":false,\"status\":400,\"error\":{\"code\":\"BAD_REQUEST\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "405", description = "허용되지 않은 메소드", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "METHOD_NOT_ALLOWED",
				value = "{\"success\":false,\"status\":405,\"error\":{\"code\":\"METHOD_NOT_ALLOWED\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "415", description = "UNSUPPORTED_MEDIA_TYPE", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "UNSUPPORTED_MEDIA_TYPE",
				value = "{\"success\":false,\"status\":415,\"error\":{\"code\":\"UNSUPPORTED_MEDIA_TYPE\",\"message\":\"잘못된 콘텐츠 타입입니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
			)
		)),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 횟수 초과", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
			examples = @ExampleObject(
				name = "TOO_MANY_REQUESTS",
				value = "{\"success\":false,\"status\":429,\"error\":{\"code\":\"TOO_MANY_REQUESTS\",\"message\":\"잘못된 요청입니다.\"},\"timestamp\":\"2025-09-02T10:45:00.123456Z\"}"
			)
		))
	})
	@GetMapping("/festivalVerify")
	public ResponseEntity<?> getFestivalVerify(@AuthenticationPrincipal UserDetailsImpl userDetails, long festivalId) {
		FestivalZoneVarifyResponse response = locationService
			.getFestivalVerify(userDetails, festivalId);
		return ResponseEntity.ok(ApiResponse.success(response));

	}

	@Operation(summary = "채팅방 영역 체크", description = "위도, 경도 정보를 받아서 기존의 Redis값과 비교한다.")
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
	public ResponseEntity<?> getChatRoomVerify(@AuthenticationPrincipal UserDetailsImpl userDetails, String chatroomId) {
		ChatRoomZoneVarifyResponse response = locationService
			.getChatroomVerify(userDetails, chatroomId);
		return ResponseEntity.ok(ApiResponse.success(response));
	}
}
