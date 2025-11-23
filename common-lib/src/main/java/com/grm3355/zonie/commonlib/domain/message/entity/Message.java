package com.grm3355.zonie.commonlib.domain.message.entity;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import com.grm3355.zonie.commonlib.domain.message.enums.MessageType;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor // Jackson이 객체 생성 시 필요
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder가 사용
@Document(collection = "messages")    // MongoDB 컬렉션 이름
@CompoundIndexes({
	// 1. 채팅 내역 조회 (과거 메시지 로드)
	@CompoundIndex(name = "idx_chatroom_created", def = "{'chatRoomId': 1, 'createdAt': -1}"),
	// 2~3번은 현재 기획상 불필요함 (우선 주석 처리)
	// 2. 이미지 모아보기
	// @CompoundIndex(name = "idx_chatroom_type_created", def = "{'chatRoomId': 1, 'type': 1, 'createdAt': -1}"),
	// 3. 핫 이미지 조회
	// @CompoundIndex(name = "idx_chatroom_type_like", def = "{'chatRoomId': 1, 'type': 1, 'likeCount': -1}")
})
public class Message {
	@Id
	private String id;            // Mongo의 ObjectId
	private String chatRoomId;        // 어느 방의 메시지인지
	private String userId;        // 누가 보냈는지
	private String nickname;
	private String content;        // 메시지 내용
	private MessageType type;        // e.g., TEXT, IMAGE
	private LocalDateTime createdAt;
	private Integer likeCount;
	private Set<String> likedByUserIds;
	@Builder.Default
	private LocalDateTime deletedAt = null; // Soft Delete 시각
}
