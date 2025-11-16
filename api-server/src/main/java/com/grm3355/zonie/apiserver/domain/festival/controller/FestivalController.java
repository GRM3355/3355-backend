package com.grm3355.zonie.apiserver.domain.festival.controller;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalCountResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalDetailResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalPageResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.RegionResponse;
import com.grm3355.zonie.apiserver.domain.festival.service.FestivalService;
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
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록 조회 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\": true,"
						+ "\"data\":{"
						+ "\"content\":["
						+ " {\"festivalId\": 119,\"title\": \"페인터즈\",\"addr1\": \"서울특별시 중구 정동길 3 (정동)\",\"eventStartDate\": \"2022-11-01\",\"eventEndDate\": \"2025-12-31\",\"firstImage\": \"\",\"lat\": 37.56813168,\"lon\": 126.9696496,\"region\": \"SEOUL\",\"chatRoomCount\": 0,\"totalParticipantCount\": 0},"
						+ " {\"festivalId\": 120,\"title\": \"서울 왕궁수문장 교대의식\",\"addr1\": \"서울특별시 중구 세종대로 99\",\"eventStartDate\": \"2022-11-01\",\"eventEndDate\": \"2025-12-31\",\"firstImage\": \"\",\"lat\": 37.56813168,\"lon\": 126.9696496,\"region\": \"SEOUL\",\"chatRoomCount\": 0,\"totalParticipantCount\": 0},"
						+ " {\"festivalId\": 121,\"title\": \"남산봉수의식 등 전통문화행사\",\"addr1\": \"서울특별시 종로구 종로 54\",\"eventStartDate\": \"2022-11-01\",\"eventEndDate\": \"2025-12-31\",\"firstImage\": \"\",\"lat\": 37.56813168,\"lon\": 126.9696496,\"region\": \"SEOUL\",\"chatRoomCount\": 0,\"totalParticipantCount\": 0},"
						+ " {\"festivalId\": 122,\"title\": \"DDP 건축투어\",\"addr1\": \"서울특별시 중구 을지로 281 (을지로7가)\",\"eventStartDate\": \"2022-11-01\",\"eventEndDate\": \"2025-12-31\",\"firstImage\": \"\",\"lat\": 37.56813168,\"lon\": 126.9696496,\"region\": \"SEOUL\",\"chatRoomCount\": 0,\"totalParticipantCount\": 0}"
						+ " ],"
						+ "\"currentPage\": 1,"
						+ "\"totalPages\": 5,"
						+ "\"totalElements\": 41,"
						+ "\"blockSize\": 10 "
						+ "},"
						+ "\"error\": null,"
						+ "\"timestamp\": \"2025-11-14T10:39:51.431745\"}"
				)
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
		FestivalDetailResponse response = festivalService.getFestivalContent(festivalId);
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "축제 지역 목록 조회", description = "축제가 열리는 지역(시/도) 목록을 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록 조회 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{"
						+ "\"success\": true,"
						+ "\"data\":["
						+ "{\"region\": \"서울\",\"code\": \"SEOUL\"},"
						+ "{\"region\": \"경기/인천\",\"code\": \"GYEONGGI\"},"
						+ "{\"region\": \"충청/대전/세종\",\"code\": \"CHUNGCHEONG\"},"
						+ "{\"region\": \"강원\",\"code\": \"GANGWON\"},"
						+ "{\"region\": \"경북/대구/울산\",\"code\": \"GYEONGBUK\"},"
						+ "{\"region\": \"경남/부산\",\"code\": \"GYEONGNAM\"},"
						+ "{\"region\": \"전라/광주\",\"code\": \"JEOLLA\"},"
						+ "{\"region\": \"제주\",\"code\": \"JEJU\"}"
						+ "],"
						+ "\"error\": null,"
						+ "\"timestamp\": \"2025-11-14T12:37:14.0061676\""
						+ "}"
				)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@GetMapping("/festivals/regions")
	public ResponseEntity<?> getFestivalRegion() {
		List<Map<String, String>> rawList = festivalService.getRegionList();
		List<RegionResponse> response = rawList.stream()
			.map(map -> new RegionResponse(map.get("region"), map.get("code")))
			.toList();
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
