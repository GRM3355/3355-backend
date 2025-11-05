package com.grm3355.zonie.commonlib.domain.chatroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import com.grm3355.zonie.commonlib.domain.user.entity.User;
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
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_user_id", nullable = false, unique = true, updatable = false)
	private Long chatRoomUserId;

	@JoinColumn(name = "chat_room_id", nullable = false)
	@ManyToOne
	private ChatRoom chatRoomId;

	@JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
	@ManyToOne
	private User user;

	@Column(name = "nick_name", nullable = false, length = 50)
	private String nickName;

}
