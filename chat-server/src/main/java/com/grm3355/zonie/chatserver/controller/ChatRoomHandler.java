package com.grm3355.zonie.chatserver.controller;

import java.util.Map;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.grm3355.zonie.chatserver.dto.MessageSendRequest;
import com.grm3355.zonie.chatserver.service.ChatRoomService;
import com.grm3355.zonie.chatserver.service.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatRoomHandler {

	private final ChatRoomService chatRoomService;
	private final MessageService messageService;
	private static final String USER_ID_ATTR = "userId";

	/**
	 * 세션 속성에서 userId를 가져오는 헬퍼 메소드
	 */
	private String getUserIdFromSession(StompHeaderAccessor accessor) {
		Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
		if (sessionAttributes == null) {
			log.error("StompHeaderAccessor sessionAttributes is null. SessionId: {}", accessor.getSessionId());
			throw new RuntimeException("Session attributes are null.");
		}

		String userId = (String) sessionAttributes.get(USER_ID_ATTR);
		if (userId == null) {
			log.warn("Cannot find userId in session attributes for session: {}", accessor.getSessionId());
			throw new RuntimeException("User not authenticated for this operation."); // WebSocketAnnotationMethodMessageHandler
		}

		return userId;
	}

	/**
	 * 채팅방 입장 (Join)
	 */
	@MessageMapping("/chat-rooms/{roomId}/join")
	public void joinRoom(
		@DestinationVariable String roomId,
		// @AuthenticationPrincipal Principal principal,
		StompHeaderAccessor accessor
	) {
		String userId = getUserIdFromSession(accessor);
		log.info(">>> STOMP RECV /app/chat-rooms/{}/join [User: {}]", roomId, userId);

		// 참여 로직 실행 (닉네임 생성, Redis Set 추가, DB 저장)
		String nickname = chatRoomService.joinRoom(userId, roomId);

	}

	/**
	 * 채팅방 퇴장 (Leave)
	 */
	@MessageMapping("/chat-rooms/{roomId}/leave")
	public void leaveRoom(
		@DestinationVariable String roomId,
		// @AuthenticationPrincipal Principal principal,
		StompHeaderAccessor accessor
	) {
		String userId = getUserIdFromSession(accessor);
		log.info(">>> STOMP RECV /app/chat-rooms/{}/leave [User: {}]", roomId, userId);

		// 퇴장 로직 실행 (Redis Set 제거, DB 삭제)
		chatRoomService.leaveRoom(userId, roomId);
	}

	/**
	 * 채팅 메시지 전송 (Send)
	 */
	@MessageMapping("/chat-rooms/{roomId}/send")
	public void sendMessage(
		@DestinationVariable String roomId,
		@Payload MessageSendRequest request,
		// @AuthenticationPrincipal Principal principal,
		StompHeaderAccessor accessor
	) {

		String userId = getUserIdFromSession(accessor);
		String content = request.getContent();

		// 1. Location-Token 헤더 검증
		// String locationToken = accessor.getFirstNativeHeader("Location-Token");
		// locationService.validateToken(userId, locationToken); // (이 로직은 나중에 추가)

		// STOMP 핸들러가 메시지를 받았는지 로그 확인
		log.debug(">>> STOMP RECV /app/chat-rooms/{}/send [User: {}, Msg: {}]",
			roomId, userId, content);

		// 2. 메시지 저장 및 발행
		messageService.sendMessage(userId, roomId, content);
	}
}
