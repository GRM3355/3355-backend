package com.grm3355.zonie.commonlib.domain.chatroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.grm3355.zonie.commonlib.global.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room_user")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class ChatRoomUser extends BaseTimeEntity {

	@Id
	@GeneratedValue
	@Column(name = "chat_room_user_id", nullable = false, unique = true, updatable = false)
	private String chatRoomUserId;

	@Column(name = "chat_room_id", nullable = false, length = 50)
	private String chatRoomId;

	@Column(name = "user_id", nullable = false, length = 50)
	private String userId;

	@Column(name = "nick_name", nullable = false, length = 50)
	private String nickName;

}
