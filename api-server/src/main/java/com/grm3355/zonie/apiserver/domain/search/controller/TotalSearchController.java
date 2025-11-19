package com.grm3355.zonie.apiserver.domain.search.controller;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomPageResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchDto;
import com.grm3355.zonie.apiserver.domain.search.dto.TotalSearchResponse;
import com.grm3355.zonie.apiserver.domain.search.service.TotalSearchService;
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
@Tag(name = "Search", description = "축제 및 채팅방 통합 검색")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TotalSearchController {

	private final TotalSearchService totalSearchService;

	@Operation(summary = "통합 검색 (축제 + 채팅방)", description = "키워드로 축제 목록과 채팅방 목록을 함께 검색합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "200",
			description = "목록 검색 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = TotalSearchResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@GetMapping("/search")
	public ResponseEntity<?> getTotalSearch(@Valid @ModelAttribute TotalSearchDto request
	) {
		TotalSearchResponse response = totalSearchService.getTotalSearch(request);
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "검색 - 채팅방 목록", description = "검색 결과 중 내 채팅방 목록만 상세 조회합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "목록 조회 성공",
			content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class),
				examples = @ExampleObject(
					name = "OK",
					value = "{\"success\": true,"
							+ "\"data\":{"
							+ "\"content\":["
							+ "{\"chatRoomId\": \"bf8cf7ed-f01e-4eb9-8bf9-5f201cbf8122\",\"festivalId\": 112,\"title\": \"채팅방\",\"lat\": 33.247109,\"lon\": 126.56447,\"festivalTitle\": \"2025 서귀포 원도심 문화페스티벌\",\"participantCount\": 0,\"lastMessageAt\": null,\"lastContent\": null}"
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
	@GetMapping("/search/chat-rooms")
	public ResponseEntity<?> getChatroomTotalSearch(@Valid @ModelAttribute ChatRoomSearchRequest request
	) {
		Page<ChatRoomResponse> pageList = totalSearchService.getChatroomTotalSearch(request);
		ChatRoomPageResponse response = new ChatRoomPageResponse(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}
}
