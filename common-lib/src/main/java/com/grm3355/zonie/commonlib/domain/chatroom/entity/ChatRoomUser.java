package com.grm3355.zonie.commonlib.domain.chatroom.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.grm3355.zonie.commonlib.domain.user.entity.User;
import com.grm3355.zonie.commonlib.global.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Table(
	name = "chat_room_user",
	uniqueConstraints = {
		@UniqueConstraint(
			columnNames = {"chat_room_id", "nick_name"}
		)
	}
)
public class ChatRoomUser extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "chat_room_user_id", nullable = false, unique = true, updatable = false)
	private Long chatRoomUserId;

	@JoinColumn(name = "chat_room_id", referencedColumnName = "chat_room_id", nullable = false)
	@ManyToOne
	@OnDelete(action = OnDeleteAction.CASCADE)
	private ChatRoom chatRoom;

	@JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
	@ManyToOne
	private User user;

	@Column(name = "nick_name", nullable = false, length = 50)
	private String nickName;

	// 사용자가 마지막으로 읽은 시각
	@Column(name = "last_read_at", nullable = false)
	private LocalDateTime lastReadAt;

	@Column(name = "is_owner", nullable = false)
	@Builder.Default
	private Boolean isOwner = false;
}
