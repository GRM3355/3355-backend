package com.grm3355.zonie.chatserver.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageSendRequest {
	private String content;
	private String tempUserId; // (임시) 인증이 구현되기 전까지 사용할 ID
	// (확장 기능: img)
}