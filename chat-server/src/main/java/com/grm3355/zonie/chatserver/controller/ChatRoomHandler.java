package com.grm3355.zonie.chatserver.controller;

import java.util.Map;

import jakarta.validation.Valid;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

import com.grm3355.zonie.chatserver.dto.ChatUserLocationDto;
import com.grm3355.zonie.chatserver.dto.MessageSendRequest;
import com.grm3355.zonie.chatserver.service.ChatLocationService;
import com.grm3355.zonie.chatserver.service.ChatRoomService;
import com.grm3355.zonie.chatserver.service.MessageService;
import com.grm3355.zonie.commonlib.global.exception.ApiErrorPayload;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatRoomHandler {

	private static final String USER_ID_ATTR = "userId";
	private final ChatRoomService chatRoomService;
	private final MessageService messageService;
	private final ChatLocationService chatLocationService;
	private final SimpMessagingTemplate messagingTemplate;

	/**
	 * 세션 속성에서 userId를 가져오는 헬퍼 메소드
	 */
	private String getUserIdFromSession(StompHeaderAccessor accessor) {
		Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
		if (sessionAttributes == null) {
			log.error("StompHeaderAccessor sessionAttributes is null. SessionId: {}", accessor.getSessionId());
			throw new RuntimeException("Session attributes are null.");
		}

		String userId = (String)sessionAttributes.get(USER_ID_ATTR);
		if (userId == null) {
			log.warn("Cannot find userId in session attributes for session: {}", accessor.getSessionId());
			throw new RuntimeException(
				"User not authenticated for this operation."); // WebSocketAnnotationMethodMessageHandler
		}

		return userId;
	}

	/**
	 * 채팅방 입장 (Join)
	 */
	@MessageMapping("/chat-rooms/{roomId}/join")
	public void joinRoom(
		@DestinationVariable String roomId,
		@Payload @Valid ChatUserLocationDto locationPayload,
		StompHeaderAccessor accessor
	) {
		String userId = getUserIdFromSession(accessor);
		log.info(">>> STOMP RECV /app/chat-rooms/{}/join [User: {}]", roomId, userId);

		// 위치 토큰 갱신/발급 시도
		// (반경 밖일 경우 토큰 발급만 건너뛰고, 예외는 던지지 않아 입장을 막지 않음.)
		chatLocationService.setLocationTokenOnJoin(
			userId,
			roomId,
			locationPayload.getLat(),
			locationPayload.getLon()
		);
		// 참여 로직 (닉네임 생성, Redis Set 추가, DB 저장) (위치 인증 성공 여부와 관계x; 항상 실행) -> API Server 대체
		// 클라이언트는 API Server의 POST /join을 먼저 호출하고 STOMP 연결을 시도해야 함.
	}

	/**
	 * 채팅방 퇴장 (Leave)
	 */
	@MessageMapping("/chat-rooms/{roomId}/leave")
	public void leaveRoom(
		@DestinationVariable String roomId,
		StompHeaderAccessor accessor
	) {
		String userId = getUserIdFromSession(accessor);
		log.info(">>> STOMP RECV /app/chat-rooms/{}/leave [User: {}]", roomId, userId);

		// 퇴장 로직 (Redis Set 제거, DB 삭제) -> API Server 대체
		// 클라이언트는 API Server의 POST /join을 먼저 호출하고 STOMP 연결을 시도해야 함.
	}

	/**
	 * 채팅 메시지 전송 (Send)
	 */
	@MessageMapping("/chat-rooms/{roomId}/send")
	public void sendMessage(
		@DestinationVariable String roomId,
		@Payload @Valid MessageSendRequest request,
		// @AuthenticationPrincipal Principal principal,
		StompHeaderAccessor accessor
	) {

		String userId = getUserIdFromSession(accessor);
		String content = request.getContent();

		// 1. Location-Token 헤더 검증
		chatLocationService.validateChatRoomEntry(userId, roomId);

		// STOMP 핸들러가 메시지를 받았는지 로그 확인
		log.debug(">>> STOMP RECV /app/chat-rooms/{}/send [User: {}, Msg: {}]",
			roomId, userId, content);

		// 2. 메시지 저장 및 발행
		messageService.sendMessage(userId, roomId, content);
	}

	/**
	 * 예외 핸들러
	 * STOMP 컨트롤러에서 발생하는 BusinessException을 처리합니다.
	 * 클라이언트의 개인 에러 큐(/user/queue/errors)로 에러 메시지를 전송합니다.
	 */
	@MessageExceptionHandler(BusinessException.class)
	public void handleBusinessException(BusinessException ex, StompHeaderAccessor accessor) {
		String userId = null;
		try {
			userId = getUserIdFromSession(accessor);
		} catch (RuntimeException e) {
			// join/send 전에 인증이 풀리는 등 userId가 없는 예외 상황에서도 로그를 남기기 위함
			log.warn("User ID not found in session during exception handling: {}", e.getMessage());
		}

		ErrorCode code = ex.errorCode();
		String message = ex.getMessage();

		// 로그: WARN 레벨
		log.warn("STOMP Validation Failed (User: {}): Code={}, Message={}", userId, code.getCode(), message);

		// api-server와 동일한 DTO로 에러 응답을 생성합니다.
		ApiErrorPayload errorPayload = new ApiErrorPayload(
			message,
			accessor.getDestination(), // 예: "/app/chat-rooms/.../send"
			null // 세부 오류 없음
		);

		// 클라이언트의 개인 에러 큐로 에러 메시지를 전송합니다.
		if (userId != null) {
			messagingTemplate.convertAndSendToUser(
				userId,
				"/queue/errors", // 클라이언트는 이 경로를 구독해야 함
				errorPayload
			);
		}
	}
}
