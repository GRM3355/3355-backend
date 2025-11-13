package com.grm3355.zonie.apiserver.domain.festival.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalCountResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalCreateRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalPageResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.RegionResponse;
import com.grm3355.zonie.apiserver.domain.festival.service.FestivalService;
import com.grm3355.zonie.apiserver.global.dto.PageResponse;
import com.grm3355.zonie.apiserver.global.swagger.ApiError400;
import com.grm3355.zonie.apiserver.global.swagger.ApiError405;
import com.grm3355.zonie.apiserver.global.swagger.ApiError415;
import com.grm3355.zonie.apiserver.global.swagger.ApiError429;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@Tag(name = "Festivals", description = "축제 목록, 상세 정보 및 지역 목록을 조회합니다.")
@RequestMapping("/api/v1")
public class FestivalController {

	private final FestivalService festivalService;

	@Operation(summary = "축제 목록 조회", description = "조건에 맞는 축제 목록을 페이지네이션하여 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "목록 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = FestivalPageResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@GetMapping("/festivals")
	public ResponseEntity<?> getFestivalList(@Valid @ModelAttribute FestivalSearchRequest request
	) {
		Page<FestivalResponse> pageList = festivalService.getFestivalList(request);
		FestivalPageResponse response = new FestivalPageResponse(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "축제 상세 조회", description = "특정 축제의 상세 정보를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "상세 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = FestivalResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@GetMapping("/festivals/{festivalId}")
	public ResponseEntity<?> gefFestivalContent(@PathVariable long festivalId,
		@ModelAttribute ChatRoomSearchRequest request
	) {
		FestivalResponse response = festivalService.getFestivalContent(festivalId);
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "축제 지역 목록 조회", description = "축제가 열리는 지역(시/도) 목록을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "지역 목록 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(type = "array", implementation = RegionResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@GetMapping("/festivals/regions")
	public ResponseEntity<?> getFestivalRegion() {
		List<?> rawList = festivalService.getRegionList();
		List<RegionResponse> response = ((List<Map<String, String>>) rawList).stream()
			.map(map -> new RegionResponse(map.get("region"), map.get("code")))
			.toList();
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "축제 생성 (테스트용)", description = "개발 테스트를 위해 새로운 축제 레코드를 생성합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",	// 테스트용이라서 201로 굳이 설정하지 않음
			description = "축제 생성 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = FestivalResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@PostMapping("/festivals")
	public ResponseEntity<?> createFestival(@RequestBody FestivalCreateRequest request) {
		FestivalResponse response = festivalService.createFestival(request);
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "지역별 축제 개수 조회", description = "특정 지역(region)의 축제 개수를 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "축제 개수 조회 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = FestivalCountResponse.class)
			)
		)
	})
	@ApiError400 // (e.g., 유효하지 않은 region 파라미터)
	@ApiError405
	@ApiError415
	@ApiError429
	@GetMapping("/festivals/count")
	public ResponseEntity<?> getFestivalCount(
		@RequestParam("region") Region region // Enum으로 받음
	) {
		long count = festivalService.getFestivalCountByRegion(region);
		return ResponseEntity.ok().body(ApiResponse.success(new FestivalCountResponse(count)));
	}
}
