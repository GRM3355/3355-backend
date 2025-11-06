package com.grm3355.zonie.chatserver.util;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import com.grm3355.zonie.commonlib.domain.auth.JwtTokenProvider;

import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

	private final JwtTokenProvider jwtTokenProvider;

	private static final String AUTHORIZATION_HEADER = "Authorization";
	private static final String BEARER_PREFIX = "Bearer ";

	@Override
	public Message<?> preSend(Message<?> message, MessageChannel channel) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

		// CONNECT 프레임일 때만 인증 수행
		if (StompCommand.CONNECT.equals(accessor.getCommand())) {

			// 1. Authorization 헤더에서 토큰 추출
			List<String> authorizationHeaders = accessor.getNativeHeader(AUTHORIZATION_HEADER);
			if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
				log.warn("STOMP Connection blocked: Missing Authorization header.");
				// 토큰 없으면 강제로 연결 종료
				throw new RuntimeException("Missing Authorization header");
			}

			String tokenWithPrefix = authorizationHeaders.get(0);
			if (tokenWithPrefix.startsWith(BEARER_PREFIX)) {
				String token = tokenWithPrefix.substring(BEARER_PREFIX.length());

				// 2. JWT 토큰 유효성 검증
				try {
					// 2-1. 성공 시 userId 추출 및 Principal 설정
					// (검증 실패 시 catch로 이동)
					jwtTokenProvider.validateToken(token);
					String userId = jwtTokenProvider.getUserIdFromToken(token);

					// 3. 인증 객체 생성
					UsernamePasswordAuthenticationToken authentication =
						new UsernamePasswordAuthenticationToken(userId, null, Collections.emptyList());

					// 4. STOMP 세션에 Principal 설정
					accessor.setUser(authentication);

					// 5. 세션 속성에 userId를 직접 저장: 동일 세션의 모든 STOMP 메시지에서 공유됨
					Map<String, Object> sessionAttributes = accessor.getSessionAttributes();
					if (sessionAttributes != null) {
						sessionAttributes.put("userId", userId);
					}
					log.info("STOMP Session Authenticated: UserId={}", userId);

				} catch (JwtException | IllegalArgumentException e) {
					// 2-2. 실패 - 예외 발생 시
					log.warn("STOMP Connection blocked: Invalid JWT token. {}", e.getMessage());
					throw new RuntimeException("Invalid or Expired JWT token");
				}
			}
		}

		return message; // CONNECT 외 다른 메시지는 그대로 통과
	}
}