package com.grm3355.zonie.chatserver.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Map;

import com.grm3355.zonie.commonlib.domain.chatroom.service.ChatRoomService;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompDisconnectEventHandler implements ApplicationListener<SessionDisconnectEvent> {

	private final ChatRoomService chatRoomService;
	private static final String USER_ID_ATTR = "userId";

	@Override
	public void onApplicationEvent(SessionDisconnectEvent event) {
		StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
		Map<String, Object> sessionAttributes = accessor.getSessionAttributes();

		if (sessionAttributes == null) {
			log.warn("Disconnect event with no session attributes. SessionId: {}", accessor.getSessionId());
			return;
		}

		String userId = (String) sessionAttributes.get(USER_ID_ATTR);

		if (userId != null) {
			log.info("STOMP Session Disconnected: UserId={}", userId);
			chatRoomService.disconnectUser(userId); // 연결 끊김
		} else {
			log.warn("Unauthenticated disconnect: SessionId={}", accessor.getSessionId());
		}
	}
}