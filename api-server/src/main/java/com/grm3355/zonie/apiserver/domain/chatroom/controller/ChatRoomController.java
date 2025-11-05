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
import com.grm3355.zonie.apiserver.common.dto.SearchRequest;
import com.grm3355.zonie.apiserver.common.jwt.UserDetailsImpl;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomRequest;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@Tag(name = "Auth & User", description = "채팅방 생성")
@RequestMapping("/api/v1/")
public class ChatRoomController {

	private final ChatRoomService chatRoomService;

	public ChatRoomController(ChatRoomService chatRoomService) {
		this.chatRoomService = chatRoomService;
	}

	//채팅방 생성
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

	//축제별 채팅방 목록
	@GetMapping("/festivals/{festivalId}/chat-rooms")
	public ResponseEntity<?> getChatRoomList(@PathVariable long festivalId,
		@ModelAttribute SearchRequest request
		) {
		Page<ChatRoomResponse> pageList = chatRoomService.getChatRoomList(festivalId, null, request);
		PageResponse<ChatRoomResponse> response = new PageResponse<>(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

	//내채팅방 목록
	@PreAuthorize("hasRole('GUEST')")
	@GetMapping("/chat-rooms/my-rooms")
	public ResponseEntity<ApiResponse<PageResponse<ChatRoomResponse>>> getChatRoomList(
		@ModelAttribute SearchRequest request, @AuthenticationPrincipal UserDetailsImpl userDetails
	) {
		String userId = userDetails.getUsername();

		Page<ChatRoomResponse> pageList = chatRoomService.getChatRoomList(0, userId, request);
		PageResponse<ChatRoomResponse> response = new PageResponse<>(pageList, request.getPageSize());
		return ResponseEntity.ok().body(ApiResponse.success(response));
	}

}
