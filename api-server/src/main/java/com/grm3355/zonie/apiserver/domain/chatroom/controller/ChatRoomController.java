package com.grm3355.zonie.apiserver.domain.chatroom.controller;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.grm3355.zonie.apiserver.domain.auth.dto.LocationDto;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomCreateResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomPageResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomSearchRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomApiService;
import com.grm3355.zonie.apiserver.global.jwt.UserDetailsImpl;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RestController
@Tag(name = "ChatRooms", description = "채팅방 생성 api와, 축제별 채팅방 목록 및 내 채팅방 목록 조회 api를 제공합니다.")
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class ChatRoomController {
	private final ChatRoomApiService chatRoomApiService;

	@Operation(summary = "채팅방 생성", description = "위치 기반으로 채팅방을 생성합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(
			responseCode = "201",
			description = "채팅방 생성 성공",
			content = @Content(
				mediaType = "application/json",
				schema = @Schema(implementation = ChatRoomCreateResponse.class)
			)
		)
	})
	@ApiError400
	@ApiError405
	@ApiError415
	@ApiError429
	@PostMapping("/festivals/{festivalId}/chat-rooms")
	@SecurityRequirement(name = "Authorization")
	public ResponseEntity<?> createChatRoom(@PathVariable long festivalId,
		@Valid @RequestBody ChatRoomRequest chatRoomRequest, HttpServletRequest servlet,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {
		// 현재 URL
		URI location = URI.create(servlet.getRequestURL().toString());
		ChatRoomCreateResponse response = chatRoomApiService.setCreateChatRoom(festivalId, chatRoomRequest,
			userDetails);
		return ResponseEntity.created(location).body(ApiResponse.success(response));
	}

	@Operation(summary = "축제별 채팅방 목록", description = "특정 축제의 채팅방을 조회합니다.")
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
	@GetMapping("/festivals/{festivalId}/chat-rooms")
	@Deprecated
	public ResponseEntity<?> getChatRoomList(@PathVariable long festivalId,
		@Valid @ModelAttribute ChatRoomSearchRequest request
	) {
		Page<ChatRoomResponse> pageList = chatRoomApiService.getFestivalChatRoomList(festivalId, request);
		ChatRoomPageResponse response = new ChatRoomPageResponse(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "내 채팅방 목록", description = "사용자 인증을 거쳐 사용자가 등록한 채팅방 목록을 조회합니다.")
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
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "Authorization")
	@GetMapping("/chat-rooms/my-rooms")
	public ResponseEntity<ApiResponse<ChatRoomPageResponse>> getChatRoomList(
		@Valid @ModelAttribute ChatRoomSearchRequest request,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {
		Page<ChatRoomResponse> pageList = chatRoomApiService.getMyChatRoomList(userDetails, request);
		ChatRoomPageResponse response = new ChatRoomPageResponse(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "채팅방 입장", description = "DB 트랜잭션을 통해 채팅방에 입장하고 memberCount를 증가시킵니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "가입 성공"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "정원 초과 또는 이미 가입됨", content = @Content(examples = @ExampleObject(name = "Conflict", value = "{\"success\": false, \"error\": {\"code\": \"CONFLICT\", \"message\": \"정원이 초과되었습니다.\"}}")))
	})
	@ApiError400
	@ApiError429
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/chat-rooms/{roomId}/join")
	public ResponseEntity<ApiResponse<String>> joinChatRoom(@PathVariable String roomId,
		@RequestBody @Valid LocationDto locationDto,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		String nickName = chatRoomApiService.joinRoom(roomId, locationDto, userDetails);
		return ResponseEntity.ok().body(ApiResponse.success(nickName)); // 닉네임을 응답으로 반환
	}

	@Operation(summary = "채팅방 퇴장", description = "DB 트랜잭션을 통해 채팅방에서 퇴장하고 ChatRoomUser를 삭제합니다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "탈퇴 성공 (No Content)"),
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음 또는 가입되어 있지 않음")
	})
	@ApiError400
	@ApiError429
	@PreAuthorize("isAuthenticated()")
	@SecurityRequirement(name = "Authorization")
	@PostMapping("/chat-rooms/{roomId}/leave")
	public ResponseEntity<?> leaveChatRoom(@PathVariable String roomId,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		chatRoomApiService.leaveRoom(roomId, userDetails);
		return ResponseEntity.noContent().build();
	}
}
