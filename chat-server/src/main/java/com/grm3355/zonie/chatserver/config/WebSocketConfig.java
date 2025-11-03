package com.grm3355.zonie.chatserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker // STOMP 메시지 브로커 기능을 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		// 프론트엔드가 웹소켓에 접속(Connect)할 엔드포인트: wss://ws.zonie.com/chat
		registry.addEndpoint("/chat")
			.setAllowedOriginPatterns("*");	// (*) MVP 단계 - CORS 전체 허용 (추후 업뎃)
			// .withSockJS(); // 구형 브라우저 지원 시 주석 지우기
	}

	public void configureMessageBroker(MessageBrokerRegistry registry) {
		// client -> server 메세지 전송 (Pub) 엔드포인트: /app
		// client가 "/app/echo"로 메시지를 보내면 @MessageMapping("/echo") 핸들러가 받음
		registry.setApplicationDestinationPrefixes("/app");

		// server -> client 메세지 수신 (Sub) 엔드포인트: /sub
		// 클라이언트가 "/sub/echo"를 구독하면, 서버는 이 주소로 메시지를 브로드캐스팅함
		// - Spring Simple (in-memory) Message Broker 사용: RedisSubscriber가 Redis에서 받은 메세지를 이 브로커(/sub)로 쏴줌: 간단하지만 유실 가능성이 있고 모니터링이 어려움
		// - Redis Pub/Sub: 지속성이 없지만 실시간성 데이터 처리에 적합.
		// - Redis는 STOMP 지원하지 않지만, Pub/Sub 기능을 통해 메세지 브로커를 사용할 수 있음
		registry.enableSimpleBroker("/sub");

	}
}
