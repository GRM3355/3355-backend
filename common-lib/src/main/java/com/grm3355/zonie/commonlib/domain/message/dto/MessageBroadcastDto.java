package com.grm3355.zonie.commonlib.domain.message.dto;

import java.time.LocalDateTime;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.enums.MessageType;

import lombok.Builder;
import lombok.Getter;

/**
 * Redis Pub/Sub으로 STOMP 구독자들에게 Publish할 때 사용하는 dto
 * likedByUserIds, liked 필드 등은 보안, 데이터 절약, 방송 불가로 제외함
 */
@Getter
@Builder
public class MessageBroadcastDto {
	private String id;
	private String chatRoomId;
	private String userId;
	private String nickname;
	private String content;
	private MessageType type;
	private LocalDateTime createdAt;
	private Integer likeCount;

	public static MessageBroadcastDto from(Message message) {
		return MessageBroadcastDto.builder()
			.id(message.getId())
			.chatRoomId(message.getChatRoomId())
			.userId(message.getUserId())
			.nickname(message.getNickname())
			.content(message.getContent())
			.type(message.getType())
			.createdAt(message.getCreatedAt())
			.likeCount(message.getLikeCount())
			.build();
	}
}
