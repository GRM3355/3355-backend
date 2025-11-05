package com.grm3355.zonie.apiserver.domain.chatroom.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@RequiredArgsConstructor
public class ChatRoomController {

	ChatRoomService chatRoomService;

	//채팅방 생성
	@PostMapping("/festivals/{festivalId}/chat-rooms")
	public ResponseEntity<?> creteChatRoom(@PathVariable long festivalId,
		@ModelAttribute ChatRoomRequest request,
		@AuthenticationPrincipal UserDetailsImpl userDetails) {

		ChatRoomResponse response = chatRoomService.setCreateChatRoom(festivalId, request, userDetails);
		return ResponseEntity.ok().body(ApiResponse.success(response));
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
	@GetMapping("/festivals/{festivalId}/my-rooms")
	public ResponseEntity<ApiResponse<PageResponse<ChatRoomResponse>>> getChatRoomList(
		@PathVariable long festivalId, @ModelAttribute SearchRequest request,
		@AuthenticationPrincipal UserDetailsImpl userDetails
	) {
		String userId = userDetails.getUsername();

		Page<ChatRoomResponse> pageList = chatRoomService.getChatRoomList(festivalId, userId, request);
		PageResponse<ChatRoomResponse> response = new PageResponse<>(pageList, request.getPageSize());

		//Page<ProductResponse> pageList = productService.getProductsUser(req, member, category);
		//PageResponse<ProductResponse> productResponse = new PageResponse<>(pageList, req.getPageSize());

		return ResponseEntity.ok().body(ApiResponse.success(response));

	}

}
