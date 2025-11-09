package com.grm3355.zonie.apiserver.domain.search.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.global.dto.PageResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchDto;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;
import com.grm3355.zonie.apiserver.domain.search.service.TotalSearchService;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "통합검색", description = "축제, 채팅방 목록을 표시합니다.")
@RequestMapping("/api/v1")
public class TotalSearchController {

	private final TotalSearchService totalSearchService;

	public TotalSearchController(TotalSearchService totalSearchService) {
		this.totalSearchService = totalSearchService;
	}

	@Operation(summary = "축제목록 + 채팅방목록", description = "해당 축제의 내용을 볼 수 있다.")
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
	})	//통합 검색
	@GetMapping("/search")
	public ResponseEntity<?> getTotalSearch(@Valid @ModelAttribute TotalSearchDto request
	) {
		TotalSearchResponse response = totalSearchService.getTotalSearch(request);
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "통합검색 - 축제 목록", description = "통합검색에서 축제목록을 볼 수 있다.")
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
	})	//통합 검색 - 축제목록
	@GetMapping("/search/festivals")
	public ResponseEntity<?> getFestivalTotalSearch(@Valid @ModelAttribute FestivalSearchRequest request
	) {
		//FestivalSearchRequest
		Page<FestivalResponse> pageList = totalSearchService.getFestivalTotalSearch(request);
		PageResponse<FestivalResponse> response = new PageResponse<>(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "통합검색 - 채팅방 목록", description = "통합검색에서 채팅방 목록을 볼 수 있다.")
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
	})	//통합 검색 - 축제목록
	@GetMapping("/search/chatrooms")
	public ResponseEntity<?> getChatroomTotalSearch(@Valid @ModelAttribute ChatRoomSearchRequest request
	) {
		Page<MyChatRoomResponse> pageList = totalSearchService.getChatroomTotalSearch(request);
		PageResponse<MyChatRoomResponse> response = new PageResponse<>(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));

	}

}
