package com.grm3355.zonie.apiserver.domain.festival.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.global.dto.PageResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.service.FestivalService;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "축제 목록 및 내용", description = "축제목록, 내용을 표시합니다.")
@RequestMapping("/api/v1")
public class FestivalController {

	private final FestivalService festivalService;

	public FestivalController(FestivalService festivalService) {
		this.festivalService = festivalService;
	}

	@Operation(summary = "축제 목록", description = "해당 축제의 목록을 볼 수 있다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록표시", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)
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
	})	//축제목록
	@GetMapping("/festivals")
	public ResponseEntity<?> getFestivalList(@ModelAttribute FestivalSearchRequest request
	) {
		Page<FestivalResponse> pageList = festivalService.getFestivalList(request);
		PageResponse<FestivalResponse> response = new PageResponse<>(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "축제 내용", description = "해당 축제의 내용을 볼 수 있다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내용표시", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)
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
	})		//축제내용
	@GetMapping("/festivals/{festivalId}")
	public ResponseEntity<?> gefFestivalContent(@PathVariable long festivalId,
		@ModelAttribute ChatRoomSearchRequest request
	) {
		FestivalResponse response = festivalService.getFestivalContent(festivalId);
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "지역목록", description = "해당 축제의 내용을 볼 수 있다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "내용표시", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)
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
	})	//지역목록
	@GetMapping("/festivals/region")
	public ResponseEntity<?> getFestivalRegion() {
		List<Region> response = festivalService.getRegionList();
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

}
