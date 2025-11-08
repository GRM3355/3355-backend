package com.grm3355.zonie.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

	private final SimpMessageSendingOperations messagingTemplate;
	private final ObjectMapper objectMapper;

	// (1) Redis의 메시지를 STOMP 브로커로 전송하는 실제 핸들러
	public class RedisSubscriber {
		public void handleEcho(String message) {
			System.out.println("LOG: Redis Pub/Sub received: " + message);
			messagingTemplate.convertAndSend("/sub/echo", message); // 메시지를 받아서 STOMP 브로커(/sub/echo)로 브로드캐스팅
		}
		public void handleMessage(String messageJson, String channel) {
			try {
				// 1. JSON 파싱 (이중 포장 풀기)
				String innerJson = objectMapper.readValue(messageJson, String.class);
				Message message = objectMapper.readValue(innerJson, Message.class);

				// --- 2. Message 객체에서 실제 roomId 가져오기 ---
				String roomId = message.getChatRoomId(); // e.g., "my-local-room"
				if (roomId == null || roomId.isEmpty()) {
					log.error("Message에 chatRoomId가 없습니다!");
					return;
				}

				// --- 3. STOMP 토픽 생성 ---
				String stompTopic = "/sub/chat-rooms/" + roomId;
				// (결과) "/sub/chat-rooms/my-local-room"
				log.info(">>> REDIS SUB RECV [Channel: {}] -> [StompTopic: {}]", channel, stompTopic);

				// 4. 해당 STOMP 토픽으로 메시지 브로드캐스팅
				messagingTemplate.convertAndSend(stompTopic, message);

			} catch (Exception e) {
				log.error("RedisSubscriber handleMessage Error", e);
			}
		}
	}

	// (2) 메시지 리스너 어댑터: 실제 핸들러(RedisSubscriber)를 연결
	// (2-A) 실제 채팅용 메시지 리스너 어댑터
	@Bean
	MessageListenerAdapter chatListenerAdapter() {
		// RedisSubscriber의 "handleMessage" 메소드가 메시지를 처리하도록 설정
		return new MessageListenerAdapter(new RedisSubscriber(), "handleMessage");
	}

	// (2-B) Echo 테스트용 메시지 리스너 어댑터
	@Bean
	MessageListenerAdapter echoListenerAdapter() {
		// RedisSubscriber의 "handleEcho" 메소드가 메시지를 처리하도록 설정
		return new MessageListenerAdapter(new RedisSubscriber(), "handleEcho");
	}

	// (3) Redis 메시지 리스너 컨테이너: 어떤 채널을 구독할지 설정
	@Bean
	RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
		MessageListenerAdapter chatListenerAdapter,
		MessageListenerAdapter echoListenerAdapter) {

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		// "chat-room:*" 패턴을 구독 (chatListenerAdapter 사용)
		container.addMessageListener(chatListenerAdapter, new PatternTopic("chat-room:*"));

		// "echo-channel" 토픽을 구독 (echoListenerAdapter 사용)
		container.addMessageListener(echoListenerAdapter, new ChannelTopic("echo-channel"));

		return container;
	}

}
