package com.grm3355.zonie.commonlib.domain.chatroom.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.locationtech.jts.geom.Point;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.user.entity.User;
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


	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	// @Column(name = "uuid", nullable = false, unique = true, updatable = false)
	// private Long Long;

	@Id
	@Column(name = "chat_room_id", unique = true, nullable = false, length = 50)
	private String chatRoomId;

	@JoinColumn(name = "festival_id", referencedColumnName = "festival_id", nullable = false)
	@ManyToOne
	private Festival festival;

	@JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
	@ManyToOne
	private User user;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "cover_image_url", nullable = false, length = 100)
	private String coverImageUrl;

	@Column(name = "max_participants", nullable = false)
	private int maxParticipants;

	@Column(name = "radius", nullable = false)
	private double radius;

	@Column(name = "position", columnDefinition = "geography(Point, 4326)")
	//@Column(name = "position", columnDefinition = "geometry(Point,4326)")
	private Point position;

}
