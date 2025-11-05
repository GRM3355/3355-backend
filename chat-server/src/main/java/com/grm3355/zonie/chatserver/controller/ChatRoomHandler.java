package com.grm3355.zonie.chatserver.controller;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.grm3355.zonie.chatserver.dto.MessageSendRequest;
import com.grm3355.zonie.commonlib.domain.message.MessageService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatRoomHandler {

	// private final ChatRoomService chatRoomService; // (feature/15)
	private final MessageService messageService;

	// ... (@MessageMapping("/join"), @MessageMapping("/leave") ... )

	/**
	 * 채팅 메시지 전송 (Send)
	 */
	@MessageMapping("/chat-rooms/{roomId}/send")
	public void sendMessage(
		@DestinationVariable String roomId,
		@Payload MessageSendRequest request,
		// @AuthenticationPrincipal UserDetailsImpl user,
		StompHeaderAccessor accessor
	) {

		String userId = request.getTempUserId();
		String content = request.getContent();

		// String userId = user.getUsername();

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
