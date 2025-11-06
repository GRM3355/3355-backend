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

import com.grm3355.zonie.apiserver.common.dto.PageResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.MyChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.SearchRequest;
import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Auth & User", description = "채팅방 생성")
@RequestMapping("/api/v1/")
public class ChatRoomController {

	private final ChatRoomService chatRoomService;

	public ChatRoomController(ChatRoomService chatRoomService) {
		this.chatRoomService = chatRoomService;
	}

	@Operation(summary = "채팅방 생성", description = "거리계산하여 채팅방을 생성할 수 있다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)
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
	@PreAuthorize("hasRole('GUEST')")
	@PostMapping("/festivals/{festivalId}/chat-rooms")
	public ResponseEntity<?> creteChatRoom(@PathVariable long festivalId,
		@Valid @RequestBody ChatRoomRequest chatRoomRequest,  HttpServletRequest servlet,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		// 현재 URL
		URI location = URI.create(servlet.getRequestURL().toString());
		ChatRoomResponse response = chatRoomService.setCreateChatRoom(festivalId, chatRoomRequest, userDetails);
		return ResponseEntity.created(location).body(ApiResponse.success(response));
	}

	@Operation(summary = "축제별 채팅방 목록", description = "해당 축제의 채팅방을 볼 수 있다.")
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
	})
	@GetMapping("/festivals/{festivalId}/chat-rooms")
	public ResponseEntity<?> getChatRoomList(@PathVariable long festivalId,
		@ModelAttribute SearchRequest request
		) {
		Page<MyChatRoomResponse> pageList = chatRoomService.getFestivalChatRoomList(festivalId, request);
		PageResponse<MyChatRoomResponse> response = new PageResponse<>(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	@Operation(summary = "내 채팅방 목록", description = "사용자의 토큰을 입력받아서 본인이 등록한 채팅방 목록을 본다.")
	@ApiResponses({
		@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 발급성공", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ApiResponse.class)
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
	@PreAuthorize("hasRole('GUEST')")
	@GetMapping("/chat-rooms/my-rooms")
	public ResponseEntity<ApiResponse<PageResponse<MyChatRoomResponse>>> getChatRoomList(
		@ModelAttribute SearchRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails
	) {
		Page<MyChatRoomResponse> pageList = chatRoomService.getMyroomChatRoomList(userDetails, request);
		PageResponse<MyChatRoomResponse> response = new PageResponse<>(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

}
