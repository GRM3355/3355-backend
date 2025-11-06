package com.grm3355.zonie.chatserver.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.chatserver.BaseIntegrationTest; // BaseIntegrationTest 상속
import com.grm3355.zonie.chatserver.dto.MessageSendRequest;
import com.grm3355.zonie.commonlib.domain.auth.JwtTokenProvider;
import com.grm3355.zonie.commonlib.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.commonlib.domain.message.Message;
import com.grm3355.zonie.commonlib.domain.message.MessageService;
import com.grm3355.zonie.commonlib.global.enums.Role;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

import java.lang.reflect.Type;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

// 1. BaseIntegrationTest를 상속받아 Testcontainers 환경 사용
class ChatRoomHandlerTest extends BaseIntegrationTest {

	@LocalServerPort
	private int port; // Spring Boot가 뜬 랜덤 포트

	@Autowired
	private JwtTokenProvider jwtTokenProvider; // 테스트 토큰 생성용

	@Autowired
	private ObjectMapper objectMapper; // JSON 직렬화용

	// 2. 실제 DB 대신 Service 레이어를 Mocking
	//    (컨트롤러 -> 서비스 호출까지만 테스트)
	@MockitoBean
	private ChatRoomService chatRoomService;
	@MockitoBean
	private MessageService messageService;

	private WebSocketStompClient stompClient;
	private StompSession stompSession;

	// 비동기 메시지 수신을 위한 큐
	private BlockingQueue<Message> messageQueue;

	private final String TEST_USER_ID = "test-user-123";
	private final String TEST_ROOM_ID = "test-room-1";

	@BeforeEach
	void setUp() throws Exception {
		this.stompClient = new WebSocketStompClient(new StandardWebSocketClient());
		// JSON 직렬화/역직렬화를 위해 ObjectMapper 설정
		MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
		converter.setObjectMapper(objectMapper);
		this.stompClient.setMessageConverter(converter);

		this.messageQueue = new LinkedBlockingDeque<>();

		// 테스트용 JWT 토큰 생성
		String token = jwtTokenProvider.createAccessToken(TEST_USER_ID, Role.GUEST);

		// STOMP CONNECT 헤더에 JWT 토큰 추가
		StompHeaders connectHeaders = new StompHeaders();
		connectHeaders.add("Authorization", "Bearer " + token);

		// 서버에 비동기 연결 (3초 대기)
		String wsUrl = "ws://localhost:" + port + "/chat";
		this.stompSession = stompClient.connectAsync(wsUrl, new StompSessionHandlerAdapter() {}, connectHeaders)
			.get(3, TimeUnit.SECONDS);

		// 연결 성공 확인
		assertThat(stompSession.isConnected()).isTrue();
	}

	@Test
	@DisplayName("채팅방 입장(/join) 시 ChatRoomService.joinRoom이 호출된다")
	void testJoinRoom() throws Exception {
		// given
		String destination = "/app/chat-rooms/" + TEST_ROOM_ID + "/join";

		// when
		// /join 엔드포인트로 메시지 전송 (Payload 없음)
		stompSession.send(destination, null);

		// then
		// ChatRoomService.joinRoom 메서드가 1초 안에 (TEST_USER_ID, TEST_ROOM_ID) 인자로 호출되었는지 검증
		verify(chatRoomService, timeout(1000).times(1))
			.joinRoom(TEST_USER_ID, TEST_ROOM_ID);
	}

	@Test
	@DisplayName("메시지 전송(/send) 시 MessageService.sendMessage가 호출된다")
	void testSendMessage() throws Exception {
		// given
		String destination = "/app/chat-rooms/" + TEST_ROOM_ID + "/send";
		String messageContent = "Hello Test";

		// 보낼 메시지(Payload) 준비
		MessageSendRequest payload = new MessageSendRequest();
		payload.setContent(messageContent);
		// tempUserId는 핸들러가 세션에서 userId를 쓰므로 무시됨

		// when
		stompSession.send(destination, payload);

		// then
		// MessageService.sendMessage가 1초 안에 올바른 인자로 호출되었는지 검증
		verify(messageService, timeout(1000).times(1))
			.sendMessage(TEST_USER_ID, TEST_ROOM_ID, messageContent);
	}

	@Test
	@DisplayName("Echo 테스트: /app/echo로 보내면 /sub/echo로 메시지가 돌아온다")
	void testEchoHandler() throws Exception {
		// given
		BlockingQueue<String> echoQueue = new LinkedBlockingDeque<>();

		// 1. /sub/echo 구독
		stompSession.subscribe("/sub/echo", new StompFrameHandler() {
			@Override
			public @NotNull Type getPayloadType(@NotNull StompHeaders headers) {
				return String.class; // Echo는 String을 반환
			}
			@Override
			public void handleFrame(@NotNull StompHeaders headers, Object payload) {
				echoQueue.add((String) payload);
			}
		});

		// when
		String message = "test echo";
		stompSession.send("/app/echo", message);

		// then
		// 큐에서 메시지가 수신될 때까지 5초간 대기
		String receivedMessage = echoQueue.poll(5, TimeUnit.SECONDS);

		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage).isEqualTo("[Echo] " + message);
	}
}
