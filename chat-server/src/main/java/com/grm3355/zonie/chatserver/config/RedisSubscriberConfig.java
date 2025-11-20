package com.grm3355.zonie.chatserver.config;

import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.chatserver.service.ChatRoomService;
import com.grm3355.zonie.commonlib.domain.message.dto.LikeUpdatePushDto;
import com.grm3355.zonie.commonlib.domain.message.dto.MessageBroadcastDto;
import com.grm3355.zonie.commonlib.domain.message.entity.Message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

	private final SimpMessageSendingOperations messagingTemplate;
	private final ObjectMapper objectMapper;
	private final ChatRoomService chatRoomService;

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

	// (2-C) 참여 이벤트 리스너 어댑터
	@Bean
	MessageListenerAdapter joinEventListenerAdapter() {
		// RedisSubscriber의 "handleJoinEvent" 메소드가 메시지를 처리하도록 설정
		return new MessageListenerAdapter(new RedisSubscriber(), "handleJoinEvent");
	}

	// (2-D) 좋아요 이벤트 리스너 어댑터
	@Bean
	MessageListenerAdapter likeEventListenerAdapter() {
		// RedisSubscriber의 "handleLikeEvent" 메소드가 메시지를 처리하도록 설정
		return new MessageListenerAdapter(new RedisSubscriber(), "handleLikeEvent");
	}

	// (2-E) 퇴장 이벤트 리스너 어댑터
	@Bean
	MessageListenerAdapter leaveEventListenerAdapter() {
		// RedisSubscriber의 "handleLeaveEvent" 메소드가 메시지를 처리하도록 설정
		return new MessageListenerAdapter(new RedisSubscriber(), "handleLeaveEvent");
	}

	// (3) Redis 메시지 리스너 컨테이너: 어떤 채널을 구독할지 설정
	@Bean
	RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory,
		MessageListenerAdapter chatListenerAdapter,
		MessageListenerAdapter echoListenerAdapter,
		MessageListenerAdapter joinEventListenerAdapter,
		MessageListenerAdapter likeEventListenerAdapter,
		MessageListenerAdapter leaveEventListenerAdapter) {

		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		// "chat-room:*" 패턴을 구독 (chatListenerAdapter 사용)
		container.addMessageListener(chatListenerAdapter, new PatternTopic("chat-room:*"));

		// "echo-channel" 토픽을 구독 (echoListenerAdapter 사용)
		container.addMessageListener(echoListenerAdapter, new ChannelTopic("echo-channel"));

		// "chat-events:join" 토픽을 구독 (joinEventListenerAdapter 사용)
		container.addMessageListener(joinEventListenerAdapter, new ChannelTopic("chat-events:join"));

		// "chat-events:like" 토픽을 구독 (likeEventListenerAdapter 사용)
		container.addMessageListener(likeEventListenerAdapter, new ChannelTopic("chat-events:like"));

		// "chat-events:leave" 토픽을 구독 (leaveEventListenerAdapter 사용)
		container.addMessageListener(leaveEventListenerAdapter, new ChannelTopic("chat-events:leave"));

		return container;
	}

	// (1) Redis의 메시지를 STOMP 브로커로 전송하는 실제 핸들러
	public class RedisSubscriber {

		public void handleEcho(String message) {
			System.out.println("LOG: Redis Pub/Sub received: " + message);
			messagingTemplate.convertAndSend("/sub/echo", message); // 메시지를 받아서 STOMP 브로커(/sub/echo)로 브로드캐스팅
		}

		/**
		 * 채팅 메세지 핸들러
		 */
		public void handleMessage(String messageJson, String channel) {
			try {
				// 1. JSON 파싱
				Message message = objectMapper.readValue(messageJson, Message.class);

				// 2. Message 객체에서 실제 roomId 가져오기
				String roomId = message.getChatRoomId(); // e.g., "my-local-room"
				if (roomId == null || roomId.isEmpty()) {
					log.error("Message에 chatRoomId가 없습니다!");
					return;
				}

				// 3. STOMP 토픽 생성
				String stompTopic = "/sub/chat-rooms/" + roomId;
				log.info(">>> REDIS SUB RECV [Channel: {}] -> [StompTopic: {}]", channel, stompTopic);

				// 4. MessageResponse DTO로 변환해 해당 STOMP 토픽으로 메시지 브로드캐스팅
				MessageBroadcastDto dto = MessageBroadcastDto.from(message);
				messagingTemplate.convertAndSend(stompTopic, dto);

			} catch (Exception e) {
				log.error("RedisSubscriber handleMessage Error", e);
			}
		}

		/**
		 * 'chat-events:join' 채널을 처리하는 핸들러 (자동 참여 이벤트)
		 */
		public void handleJoinEvent(String messageJson) {
			try {
				// 1. JSON 파싱
				Map<String, String> event = objectMapper.readValue(messageJson, Map.class);
				String userId = event.get("userId");
				String roomId = event.get("roomId");
				String nickName = event.get("nickName");

				if (userId == null || roomId == null || nickName == null) {
					log.error("Invalid join event message: {}", messageJson);
					return;
				}

				log.info(">>> REDIS SUB RECV [Channel: chat-events:join] -> User: {}, Room: {}, Nickname: {}", userId,
					roomId, nickName);

				// 2. ChatRoomService의 joinRoom 로직 호출
				chatRoomService.joinRoom(userId, roomId, nickName);

			} catch (Exception e) {
				log.error("RedisSubscriber handleJoinEvent Error", e);
			}
		}

		/**
		 * 'chat-events:like' 채널을 처리하는 핸들러 (좋아요 이벤트)
		 */
		public void handleLikeEvent(String messageJson) {
			try {
				// 1. JSON 파싱
				LikeUpdatePushDto dto = objectMapper.readValue(messageJson, LikeUpdatePushDto.class);

				// 2. dto 객체에서 실제 roomId 가져오기
				String roomId = dto.getRoomId();
				if (roomId == null || roomId.isEmpty()) {
					log.error("LikeUpdatePushDto에 roomId가 없습니다!");
					return;
				}

				// 3. STOMP 토픽 생성
				String stompTopic = "/sub/chat-rooms/" + roomId;
				log.info(">>> REDIS SUB RECV [Channel: chat-events:like] -> [StompTopic: {}]", stompTopic);

				// 4. 해당 STOMP 토픽으로 LikeUpdatePushDto 브로드캐스팅
				messagingTemplate.convertAndSend(stompTopic, dto);
			} catch (Exception e) {
				log.error("RedisSubscriber handleLikeEvent Error", e);
			}
		}

		/**
		 * 'chat-events:leave' 채널을 처리하는 핸들러 (명시적 퇴장 이벤트)
		 */
		// 이 메서드는 MessageListenerAdapter를 통해 RedisSubscriber 클래스에서 호출될 것입니다.
		public void handleLeaveEvent(String messageJson) {
			try {
				// 1. JSON 파싱: join 이벤트와 동일하게 userId와 roomId를 추출한다고 가정
				Map<String, String> event = objectMapper.readValue(messageJson, Map.class);
				String userId = event.get("userId");
				String roomId = event.get("roomId");

				if (userId == null || roomId == null) {
					log.error("Invalid leave event message: {}", messageJson);
					return;
				}

				log.info(">>> REDIS SUB RECV [Channel: chat-events:leave] -> User: {}, Room: {}", userId, roomId);

				// 2. ChatRoomService의 leaveRoom 로직 호출
				chatRoomService.leaveRoom(userId, roomId);

			} catch (Exception e) {
				log.error("RedisSubscriber handleLeaveEvent Error", e);
			}
		}
	}
}
