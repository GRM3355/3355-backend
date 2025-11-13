package com.grm3355.zonie.chatserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * chat-server의 인증 흐름
 * 클라이언트 <-> (HTTPS/WSS) <-> 로드 밸런서 (AWS ALB 등) <-> (HTTP/WS) <-> chat-server 컨테이너
 * 1. 클라이언트가 wss://...로 접속 시도
 * 2. TLS 핸드 셰이크: 클라이언트(브라우저)가 서버(ALB)가 SSL 인증서를 사용해 암호화 통신 협상 - 암호화된 터널이 생성됨
 * 3. WebSocket 핸드셰이크: 암호화된 터널 내에서 GET /chat 요청이 전송되면 chat-server의 Spring Security가 이 요청을 받고 설정에 따라 해당 핸드셰이크 허용
 * 		=> HTTP 핸드셰이크 (* 이 파일): 기본 인증을 끄고 requestMatchers("/chat/**").permitAll()로 이 HTTP 요청을 통과
 * 4. STOMP 인증 연결: HTTP 연결이 성공하면 클라이언트가 STOMP에 CONNECT 프레임과 함께 헤더에 Authorization: Bearer ... 토큰을 함께 보냄
 * 		=> JwtChannelInterceptor가 CONNECT 메세지 가로채고 헤더에서 토큰을 꺼내 유효성을 검증함
 */
@Configuration("chatServerSecurityConfig")
public class SecurityConfig {

	private static final String[] WHITE_LIST = {
		"/",
		"/health",
		"/actuator/**",
		"/chat/**"
	};

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http
			// 1. CSRF 비활성화 (STOMP는 기본적으로 CSRF 토큰을 사용하기 어려움)
			.csrf(AbstractHttpConfigurer::disable)

			// 2. HTTP Basic Auth 비활성화 (가장 중요!)
			.httpBasic(AbstractHttpConfigurer::disable)

			// 3. Form Login 비활성화
			.formLogin(AbstractHttpConfigurer::disable)

			// 4. 세션을 사용하지 않음 (STATELESS)
			.sessionManagement(session ->
				session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			)

			// 5. 경로별 접근 권한 설정
			.authorizeHttpRequests(auth -> auth
				// 웹소켓 핸드셰이크 경로는 모두 허용
				.requestMatchers(WHITE_LIST).permitAll()
				.anyRequest().denyAll()
			);

		return http.build();
	}
}