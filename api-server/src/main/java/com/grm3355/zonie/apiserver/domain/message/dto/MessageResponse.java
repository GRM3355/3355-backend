package com.grm3355.zonie.apiserver.domain.message.dto;

import java.time.LocalDateTime;
import java.util.Set;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class MessageResponse {

	private String id;              // Message ID

	@Schema(description = "채팅방 아이디", example = "room:abc-def-ghi")
	private String chatRoomId;

	@Schema(description = "사용자 아이디", example = "user:asdf-asdf-asdf-aasdf")
	private String userId;          // 메세지 작성자의 User ID

	@Schema(description = "닉네임", example = "#3355")
	private String nickname;

	@Schema(description = "메세지 내용", example = "하이")
	private String content;

	@Schema(description = "메세지 타입", example = "TEXT")
	private String type;

	@Schema(description = "메세지 전송 시간", example = "2025-11-19 00:00:00")
	private LocalDateTime createdAt;

	// 레디스로부터 읽을 실시간 데이터
	@Schema(description = "좋아요 수", example = "23")
	private int likeCount;

	@Schema(description = "현재 사용자가 이 메세지에 좋아요를 눌렀는가", example = "true")
	private boolean liked;

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
