package com.grm3355.zonie.commonlib.domain.message.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class LikeUpdatePushDto {
	private String roomId;       	// STOMP 라우팅에 필요
	private String messageId;    	// 어느 메시지인지
	private String userId;       	// 누가 눌렀는지
	private boolean liked;       	// 현재 좋아요 상태 (true/false)
	private long likeCount;    		// 최종 좋아요 개수
}
