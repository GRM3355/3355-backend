package com.grm3355.zonie.chatserver.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import com.grm3355.zonie.chatserver.util.JwtChannelInterceptor;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSocketMessageBroker // STOMP 메시지 브로커 기능을 활성화
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

	private final JwtChannelInterceptor jwtChannelInterceptor;

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
		// RedisSubscriber가 Redis(Pub/Sub)에서 받은 메시지를
		// 이곳에 등록된 SimpleBroker(/sub)로 전송하여 클라이언트에게 브로드캐스팅함.
		registry.enableSimpleBroker("/sub");

	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		// 클라이언트에서 들어오는 메시지 (CONNECT, SEND, SUBSCRIBE 등)를 가로챕니다.
		registration.interceptors(jwtChannelInterceptor);
	}
}
