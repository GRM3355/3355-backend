package com.grm3355.zonie.e2e;

import static org.assertj.core.api.Assertions.*;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import com.grm3355.zonie.ApiServerApplication;
import com.grm3355.zonie.ChatServerApplication;
import com.grm3355.zonie.apiserver.domain.auth.dto.AuthResponse;
import com.grm3355.zonie.apiserver.domain.chatroom.dto.ChatRoomResponse;
import com.grm3355.zonie.apiserver.domain.message.dto.MessageResponse;
import com.grm3355.zonie.apiserver.global.config.DataAccessConfig;
import com.grm3355.zonie.chatserver.config.RedisConfig;
import com.grm3355.zonie.chatserver.config.SecurityConfig;
import com.grm3355.zonie.chatserver.controller.HealthCheckController;
import com.grm3355.zonie.chatserver.dto.MessageSendRequest;
import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.repository.MessageRepository;
import com.grm3355.zonie.commonlib.global.response.ApiResponse;
import com.grm3355.zonie.commonlib.global.util.RedisScanService;

/**
 * [E2E 테스트 시나리오: 5단계 - 과거 메시지 조회]
 *
 * 1. (API) 사용자 인증 (위치 기반 토큰 발급)
 * 2. (API) 채팅방 생성 (미리 준비된 Festival ID 사용)
 * 3. (STOMP) WebSocket 연결 및 채팅방 구독
 * 4. (STOMP) 메시지 25개 전송 (페이징 확인용)
 * 5. (API) 3번째 메시지 '좋아요' 누르기
 * 6. (API) 5단계 (P1) API 검증 (데이터 20개, 3번째 메시지 '좋아요' 반영 확인)
 * 7. (API) 5단계 (P2) API 검증 (데이터 5개, hasNext=false 확인)
 *
 * 사전 준비:
 * 1. `chat-server` (8081)가 로컬에서 실행 중이어야 합니다.
 * 2. `psql` 테스트 DB에 `FESTIVAL_ID = 1L`인 축제 데이터가 존재해야 합니다.
 * (위도/경도: 37.5845, 126.9780 근처)
 */

@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	classes = {
		ApiServerApplication.class,
		ChatServerApplication.class,
		MessageFeatureE2ETest.E2ETestConfig.class
	})
@TestPropertySource(
	properties = {
		"spring.main.allow-bean-definition-overriding=true",
		"chat.pre-create-day=7",
		"chat.pre-view-day=7",
		"chat.max-chat-person=300",
		"chat.max-chat-room=30",
		"chat.radius=1.0"
	}
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MessageFeatureE2ETest {

	private static final Logger log = LoggerFactory.getLogger(MessageFeatureE2ETest.class);

	// --- 설정 ---
	private static final String CHAT_SERVER_PORT = "8081";
	private static final String CHAT_WEBSOCKET_URL = "ws://localhost:" + CHAT_SERVER_PORT + "/chat";
	private static final long TEST_FESTIVAL_ID = 1L;		// [!!] DB에 미리 존재해야 함
	private static final double TEST_LAT = 37.5845;
	private static final double TEST_LON = 126.9780;
	private static final int TOTAL_MESSAGES = 25;
	private static final int PAGE_SIZE = 20;

	// --- 서버 포트 및 HTTP 클라이언트 ---
	@LocalServerPort
	private int apiServerPort;

	@Autowired
	private TestRestTemplate restTemplate; // Postman 역할

	// --- STOMP 클라이언트 ---
	private WebSocketStompClient stompClient;
	private StompSession stompSession;

	// --- DB/Redis 검증 (선택적) ---
	@Autowired
	private MessageRepository messageRepository; // MongoDB

	// --- 테스트 간 공유 데이터 ---
	private String userToken;
	private String chatRoomId;
	private final BlockingQueue<Message> receivedMessages = new LinkedBlockingDeque<>();
	private String targetMessageId; 	// '좋아요' 누를 메시지 ID
	private String page1CursorId;   	// P1의 마지막 메시지 ID (커서)

	@BeforeAll
	void globalSetUp() throws Exception {
		log.warn("===========================================================================");
		log.warn("[E2E Test] 사전 준비: 'chat-server' (8081)가 실행 중이어야 합니다.");
		log.warn("[E2E Test] 사전 준비: TEST DB에 Festival(ID={})가 존재해야 합니다.", TEST_FESTIVAL_ID);
		log.warn("===========================================================================");

		// --- 1. (API) 사용자 인증 (토큰 발급) ---
		String authUrl = "http://localhost:" + apiServerPort + "/api/v1/auth/token-register";
		Map<String, Double> authRequest = Map.of("lat", TEST_LAT, "lon", TEST_LON);

		ResponseEntity<ApiResponse<AuthResponse>> authResponse = restTemplate.exchange(
			authUrl,
			HttpMethod.POST,
			new HttpEntity<>(authRequest),
			new ParameterizedTypeReference<ApiResponse<AuthResponse>>() {}
		);

		assertThat(authResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		this.userToken = authResponse.getBody().getData().accessToken();
		assertThat(this.userToken).isNotNull();

		log.info("[E2E 1/7] 사용자 인증 성공 (Token: {}...)", this.userToken.substring(0, 10));

		// --- 2. (API) 채팅방 생성 ---
		String roomUrl = "http://localhost:" + apiServerPort + "/api/v1/festivals/" + TEST_FESTIVAL_ID + "/chat-rooms";
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(this.userToken);
		ResponseEntity<ApiResponse<ChatRoomResponse>> roomResponse = restTemplate.exchange(
			roomUrl,
			HttpMethod.POST,
			new HttpEntity<>(headers),
			new ParameterizedTypeReference<ApiResponse<ChatRoomResponse>>() {}
		);

		assertThat(roomResponse.getStatusCode()).isEqualTo(HttpStatus.OK); // 또는 CREATED
		this.chatRoomId = roomResponse.getBody().getData().getChatRoomId();
		assertThat(this.chatRoomId).isNotNull();

		log.info("[E2E 2/7] 채팅방 생성 성공 (RoomId: {})", this.chatRoomId);

		// --- 3. (STOMP) WebSocket 연결 및 채팅방 구독 ---
		this.stompClient = new WebSocketStompClient(new SockJsClient(
			List.of(new WebSocketTransport(new StandardWebSocketClient()))
		));

		StompHeaders connectHeaders = new StompHeaders();
		connectHeaders.add("Authorization", "Bearer " + userToken);

		this.stompSession = stompClient.connectAsync(
			CHAT_WEBSOCKET_URL,
			null,
			connectHeaders,
			Optional.of(new StompSessionHandlerAdapter() {
			})
		).get(10, TimeUnit.SECONDS); 	// 10초 대기

		log.info("[E2E 3/7] STOMP 연결 성공 (SessionId: {})", this.stompSession.getSessionId());

		// 채팅방 구독 (메시지 수신 대기)
		stompSession.subscribe("/sub/chat-rooms/" + chatRoomId, new StompFrameHandler() {
			@Override
			public Type getPayloadType(StompHeaders headers) {
				// MessageService가 Message 엔티티를 그대로 발행
				return Message.class;
			}

			@Override
			public void handleFrame(StompHeaders headers, Object payload) {
				receivedMessages.offer((Message) payload);
			}
		});

		log.info("[E2E 3/7] STOMP 구독 시작 (/sub/chat-rooms/{})", this.chatRoomId);
	}

	@Test
	@Order(1)
	@DisplayName("STOMP 메시지 25개 전송 및 '좋아요' API 호출 (데이터 준비)")
	void step1_PrepareData() throws Exception {

		// --- 4. (STOMP) 메시지 25개 전송 ---
		String sendDestination = "/app/chat-rooms/" + chatRoomId + "/send";

		for (int i = 1; i <= TOTAL_MESSAGES; i++) {
			MessageSendRequest payload = new MessageSendRequest();
			payload.setContent("테스트 메시지 " + i);
			stompSession.send(sendDestination, payload);
			Thread.sleep(100); // 메시지 순서 보장 및 DB/Redis 전파 시간 확보
		}

		log.info("[E2E 4/7] STOMP 메시지 {}개 전송 완료", TOTAL_MESSAGES);

		// --- 4-1. 전송한 메시지 ID 확보 ---

		for (int i = 0; i < TOTAL_MESSAGES; i++) {
			Message msg = receivedMessages.poll(5, TimeUnit.SECONDS);
			assertThat(msg).isNotNull();
			if (i == 2) { // 3번째 메시지
				this.targetMessageId = msg.getId().toHexString();
			}

			if (i == (PAGE_SIZE - 1)) { // 20번째 메시지 (P1의 마지막)
				this.page1CursorId = msg.getId().toHexString();
			}
		}

		assertThat(this.targetMessageId).isNotNull();
		assertThat(this.page1CursorId).isNotNull();

		log.info("[E2E 4/7] '좋아요' 대상 ID: {}", this.targetMessageId);
		log.info("[E2E 4/7] P1 커서 ID: {}", this.page1CursorId);

		// --- 5. (API) 3번째 메시지 '좋아요' 누르기 ---
		String likeUrl = "http://localhost:" + apiServerPort + "/api/v1/messages/" + this.targetMessageId + "/like";
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(this.userToken);

		ResponseEntity<ApiResponse<Map<String, Object>>> likeResponse = restTemplate.exchange(
			likeUrl,
			HttpMethod.POST,
			new HttpEntity<>(headers),
			new ParameterizedTypeReference<ApiResponse<Map<String, Object>>>() {}
		);

		assertThat(likeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(likeResponse.getBody().getData().get("liked")).isEqualTo(true);
		assertThat(likeResponse.getBody().getData().get("likeCount")).isEqualTo(1);

		log.info("[E2E 5/7] '좋아요' API 호출 성공");
	}



	@Test
	@Order(2)
	@DisplayName("5단계 (P1) API 검증 (1페이지 / 20개)")
	void step2_TestPage1() {
		// --- 6. (API) 5단계 (P1) API 검증 ---
		String getUrl = "http://localhost:" + apiServerPort + "/api/v1/chat-rooms/" + this.chatRoomId + "/messages";
		HttpHeaders headers = new HttpHeaders();
		headers.setBearerAuth(this.userToken);

		// ApiResponse<Slice<MessageResponse>> 타입 파싱
		ParameterizedTypeReference<ApiResponse<Slice<MessageResponse>>> p1ResponseType =
			new ParameterizedTypeReference<>() {};

		ResponseEntity<ApiResponse<Slice<MessageResponse>>> p1Response = restTemplate.exchange(
			getUrl,
			HttpMethod.GET,
			new HttpEntity<>(headers),
			p1ResponseType
		);

		assertThat(p1Response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Slice<MessageResponse> slice = p1Response.getBody().getData();
		assertThat(slice.getContent().size()).isEqualTo(PAGE_SIZE);
		assertThat(slice.hasNext()).isTrue();

		// 3번째로 보낸 메시지 = 25개 중 23번째 (인덱스 22) = 1페이지에서는 18번째 (인덱스 17)
		// (25, 24, 23, ... , 6) -> 20개
		// (인덱스 0 = 25번째 메시지, 인덱스 19 = 6번째 메시지)
		// (3번째 메시지는 25, 24, *23* ... 이므로 P1에 없음)

		// [검증 로직 수정] 메시지 순서 재계산
		// 1, 2, 3, ... 20 (P1), 21, ... 25 (P2)

		// MongoDB 조회는 createdAt 내림차순 (최신순)
		// 25, 24, ..., 6 (P1 / 20개) -> P1의 마지막(인덱스 19)은 '6번째' 메시지. 커서는 6번째.

		// 5, 4, 3, 2, 1 (P2 / 5개) -> P2의 3번째(인덱스 2)는 '3번째' 메시지

		// P1 검증

		assertThat(slice.getContent().get(0).getContent()).isEqualTo("테스트 메시지 25"); // 가장 최신
		assertThat(slice.getContent().get(19).getContent()).isEqualTo("테스트 메시지 6");

		// '좋아요' 누른 3번째 메시지는 P1에 없어야 함
		log.info("[E2E 6/7] 5단계 (P1) API 검증 성공 (20개, hasNext=true)");

		// P1의 마지막 메시지(6번째) ID를 커서로 사용
		this.page1CursorId = slice.getContent().get(19).getId(); // '6번째' 메시지 ID
	}

	@Test
	@Order(3)
	@DisplayName("5단계 (P2) API 검증 (2페이지 / 5개 / '좋아요' 반영)")
	void step3_TestPage2() {
		// --- 7. (API) 5단계 (P2) API 검증 (커서 기반) ---
		// P1의 마지막 메시지(6번째)의 ID를 커서로 사용
		assertThat(this.page1CursorId).as("P1 테스트(step2)가 먼저 실행되어야 함").isNotNull();
		assertThat(this.targetMessageId).as("데이터 준비(step1)가 먼저 실행되어야 함").isNotNull();

		String getUrlP2 = "http://localhost:" + apiServerPort + "/api/v1/chat-rooms/" + this.chatRoomId + "/messages?before=" + this.page1CursorId;
		HttpHeaders headers = new HttpHeaders();

		headers.setBearerAuth(this.userToken);

		ParameterizedTypeReference<ApiResponse<Slice<MessageResponse>>> p2ResponseType =
			new ParameterizedTypeReference<>() {};

		ResponseEntity<ApiResponse<Slice<MessageResponse>>> p2Response = restTemplate.exchange(
			getUrlP2,
			HttpMethod.GET,
			new HttpEntity<>(headers),
			p2ResponseType
		);

		assertThat(p2Response.getStatusCode()).isEqualTo(HttpStatus.OK);
		Slice<MessageResponse> slice = p2Response.getBody().getData();

		// 5, 4, 3, 2, 1 (5개)
		assertThat(slice.getContent().size()).isEqualTo(5);
		assertThat(slice.hasNext()).isFalse();

		// P2의 첫번째(인덱스 0) = 5번째 메시지
		assertThat(slice.getContent().get(0).getContent()).isEqualTo("테스트 메시지 5");

		// P2의 세번째(인덱스 2) = 3번째 메시지
		MessageResponse likedMessage = slice.getContent().get(2);
		assertThat(likedMessage.getId()).isEqualTo(this.targetMessageId);

		// [핵심 검증] 5단계 API가 Redis의 실시간 '좋아요' 데이터를 덮어썼는지 확인
		assertThat(likedMessage.getContent()).isEqualTo("테스트 메시지 3");
		assertThat(likedMessage.isLiked()).isTrue();     		// [검증] liked: true
		assertThat(likedMessage.getLikeCount()).isEqualTo(1); 	// [검증] likeCount: 1

		log.info("[E2E 7/7] 5단계 (P2) API 검증 성공 (5개, hasNext=false, '좋아요' 반영)");
	}

	/**
	 * E2E 테스트를 위한 커스텀 Spring Boot 설정.
	 * ApiServerApplication의 기본 @ComponentScan 설정을 "대체"하여,
	 * chat-server의 컨트롤러를 스캔에서 제외합니다.
	 */
	@Configuration
	@ComponentScan(
		basePackages = "com.grm3355.zonie",
		excludeFilters = @ComponentScan.Filter(
			type = FilterType.ASSIGNABLE_TYPE,
			classes = {	// 충돌 방지
				HealthCheckController.class,
				SecurityConfig.class,
				RedisConfig.class,
				RedisScanService.class,
				DataAccessConfig.class
			}
		)
	)
	public static class E2ETestConfig {
		// 이 클래스는 E2E 테스트용 메인 설정 클래스 역할을 합니다.
	}
}
