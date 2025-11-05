package com.grm3355.zonie.commonlib.domain.message;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor // Jackson이 객체 생성 시 필요
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder가 사용
@Document(collection = "messages") // MongoDB 컬렉션 이름
public class Message {

	@Id
	private String id; // Mongo의 ObjectId
	private String chatRoomId; // 어느 방의 메시지인지 (인덱싱 필요)
	private String userId;       // 누가 보냈는지
	private String nickname;     // (Join/Leave 기능 완성 후 채워넣을 필드)
	private String content;      // 메시지 내용
	private MessageType type;    // e.g., TEXT, IMAGE
	private LocalDateTime createdAt;

	// (확장 기능: 좋아요)
	// private Integer likeCount;
	// private Set<String> likedByUserIds;
}
