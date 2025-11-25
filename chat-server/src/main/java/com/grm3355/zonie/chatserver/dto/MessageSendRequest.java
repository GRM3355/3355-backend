package com.grm3355.zonie.chatserver.dto;

import jakarta.validation.constraints.Size;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class MessageSendRequest {
	@Size(max = 500, message = "메시지는 최대 500자까지 보낼 수 있습니다.")
	private String content;
}
