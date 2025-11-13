package com.grm3355.zonie.chatserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Redis에 저장된 JSON을 역직렬화하기 위한 DTO
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // JSON에 있지만 여기 없는 필드는 무시
public class ChatUserLocationDto {
	private String userId;
	private double lat;
	private double lon;
	private long timestamp;
	// clientIp, device 등: 검증에 필요 없음 - 생략
}