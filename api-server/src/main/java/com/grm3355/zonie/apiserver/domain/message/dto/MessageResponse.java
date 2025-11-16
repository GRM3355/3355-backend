package com.grm3355.zonie.apiserver.domain.message.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MessageResponse {

	private String id;              // Message ID
	private String chatRoomId;
	private String userId;          // 메세지 작성자의 User ID
	private String nickname;
	private String content;
	private String type;
	private LocalDateTime createdAt;

	// 레디스로부터 읽을 실시간 데이터
	private int likeCount;
	private boolean liked; // Did the *current* user like this message?

	// DTO
	public static MessageResponse from(Message message, int liveLikeCount, Set<String> likedByUserIds,
		String currentUserId) {

		boolean isLikedByCurrentUser = false;
		if (likedByUserIds != null) {
			isLikedByCurrentUser = likedByUserIds.contains(currentUserId);
		}

		return MessageResponse.builder()
			.id(message.getId())
			.chatRoomId(message.getChatRoomId())
			.userId(message.getUserId())
			.nickname(message.getNickname())
			.content(message.getContent())
			.type(message.getType().name())
			.createdAt(message.getCreatedAt())
			.likeCount(liveLikeCount)
			.liked(isLikedByCurrentUser)
			.build();
	}
}
