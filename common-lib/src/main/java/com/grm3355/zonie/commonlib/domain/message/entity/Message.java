package com.grm3355.zonie.commonlib.domain.message.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Set;

import com.grm3355.zonie.commonlib.domain.message.enums.MessageType;

@Getter
@Setter
@Builder
@NoArgsConstructor // Jackson이 객체 생성 시 필요
@AllArgsConstructor(access = AccessLevel.PRIVATE) // Builder가 사용
@Document(collection = "messages") 	// MongoDB 컬렉션 이름
public class Message {
	@Id
	private String id;			// Mongo의 ObjectId
	private String chatRoomId; 		// 어느 방의 메시지인지
	private String userId;       	// 누가 보냈는지
	private String nickname;
	private String content;      	// 메시지 내용
	private MessageType type;    	// e.g., TEXT, IMAGE
	private LocalDateTime createdAt;
	private Integer likeCount;
	private Set<String> likedByUserIds;
}
