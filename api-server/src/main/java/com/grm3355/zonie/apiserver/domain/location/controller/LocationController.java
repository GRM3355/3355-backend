package com.grm3355.zonie.apiserver.domain.location.controller;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.auth.dto.LocationTokenResponse;
import com.grm3355.zonie.apiserver.domain.location.service.LocationService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
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

	private final LocationService locationService;

	@Operation(summary = "축제 위치 인증 및 토큰 발급", description = "사용자의 현재 위도/경도를 축제 반경과 비교하여 위치 인증 토큰(15분)을 발급받습니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "위치 인증 성공. 토큰 발급/갱신 완료.",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\":true,\"data\":{\"message\":\"인증 성공. (0.50km / 반경 1.00km)\"},\"timestamp\":\"2025-09-02T10:30:00.123456Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "400",
			description = "입력값 유효성 검증 실패 (lat/lon 누락)",
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
			responseCode = "403",
			description = "축제 반경 외부에 있음",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "FORBIDDEN",
					value = "{\"success\":false,\"status\":403,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"축제 반경(1.00km) 외부에 있습니다. (현재 거리: 1.50km)\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "404",
			description = "축제 정보를 찾을 수 없음",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "NOT_FOUND",
					value = "{\"success\":false,\"status\":404,\"error\":{\"code\":\"NOT_FOUND\",\"message\":\"관련 축제정보가 없습니다.\"},\"timestamp\":\"2025-09-02T10:35:00.987654Z\"}"
				)
			)
		)
	})
	@PostMapping("/verify/festival/{festivalId}")
	public ResponseEntity<?> verifyFestivalLocation(
		@AuthenticationPrincipal UserDetailsImpl userDetails,
		@PathVariable long festivalId,
		@Valid @RequestBody LocationDto locationDto) { // lat/lon을 Body로 받음

		LocationTokenResponse response = locationService.verifyAndGenerateToken(
			userDetails, festivalId, locationDto
		);
		return ResponseEntity.ok(ApiResponse.success(response));
	}

	// [삭제] @PutMapping("/update")
	// -> 축제 단위 인증으로 변경되어 더 이상 필요x

	// [삭제] @GetMapping("/festivalVerify")
	// -> POST /verify/festival/{festivalId}로 통합

	// [삭제] @GetMapping("/chatroomVerify")
	// -> 축제 단위 인증으로 변경되어 더 이상 필요x
}
