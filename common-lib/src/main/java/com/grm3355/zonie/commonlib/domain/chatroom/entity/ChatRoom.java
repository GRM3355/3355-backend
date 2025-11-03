package com.grm3355.zonie.commonlib.domain.chatroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.springframework.data.geo.Point;

import com.grm3355.zonie.commonlib.global.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "chat_room")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class ChatRoom extends BaseTimeEntity {

	@Id
	@GeneratedValue
	@Column(name = "uuid", nullable = false, unique = true, updatable = false)
	private Long Long;

	@Column(name = "chat_room_id", nullable = false, length = 50)
	private String chatRoomId;

	@Column(name = "festival_id", nullable = false, length = 50)
	private long festivalId;

	@Column(name = "user_id", nullable = false, length = 50)
	private String userId;

	@Column(name = "100", nullable = false, length = 50)
	private String title;

	@Column(name = "cover_image_url", nullable = false, length = 50)
	private String coverImageUrl;

	@Column(name = "max_participants", nullable = false)
	private int maxParticipants;

	@Column(name = "radius", nullable = false)
	private double radius;

	@Column(name = "position", columnDefinition = "geography(Point, 4326)")
	private Point position;
}
