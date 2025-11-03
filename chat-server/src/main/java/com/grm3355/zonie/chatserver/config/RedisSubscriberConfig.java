package com.grm3355.zonie.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class RedisSubscriberConfig {

	private final SimpMessageSendingOperations messagingTemplate;

	// (1) Redis의 메시지를 STOMP 브로커로 전송하는 실제 핸들러
	public class RedisSubscriber {
		public void handleMessage(String message) {
			System.out.println("LOG: Redis Pub/Sub received: " + message);
			messagingTemplate.convertAndSend("/sub/echo", message); // 메시지를 받아서 STOMP 브로커(/sub/echo)로 브로드캐스팅
		}
	}

	// (2) 메시지 리스너 어댑터: 실제 핸들러(RedisSubscriber)를 연결
	@Bean
	MessageListenerAdapter listenerAdapter() {
		// RedisSubscriber의 handleMessage 메소드가 메시지를 처리하도록 설정
		return new MessageListenerAdapter(new RedisSubscriber(), "handleMessage");
	}

	// (3) Redis 메시지 리스너 컨테이너: 어떤 채널을 구독할지 설정
	@Bean
	RedisMessageListenerContainer redisContainer(RedisConnectionFactory connectionFactory, MessageListenerAdapter listenerAdapter) {
		RedisMessageListenerContainer container = new RedisMessageListenerContainer();
		container.setConnectionFactory(connectionFactory);

		// "echo-channel"이라는 토픽(채널)을 구독
		container.addMessageListener(listenerAdapter, new ChannelTopic("echo-channel"));

		return container;
	}

}
