package com.grm3355.zonie.chatserver.websocket;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class EchoHandler {

	private final RedisTemplate<String, String> redisTemplate;

	/**
	 * Spike: WS 연결 및 Redis Pub/Sub 연동 테스트
	 * 1. Client가 "/app/echo"로 메세지 보냄 (Pub)
	 * 2. 이 핸들러가 메세지를 받고, RedisTemplate을 사용해 "echo-channel"이라는 채널로 메시지를 발행함 (Pub)
	 * 3. RedisSubscriber가 이 메세지를 받아 /sub/echo로 브로드캐스팅함.
	 */
	@MessageMapping("/echo")
	public void echo(String message) {
		System.out.println("LOG: /app/echo received: " + message);
		redisTemplate.convertAndSend("echo-channel", "[Echo] " + message); // StompBrokerRelay 대신 RedisTemplate으로 직접 Pub
	}
}
