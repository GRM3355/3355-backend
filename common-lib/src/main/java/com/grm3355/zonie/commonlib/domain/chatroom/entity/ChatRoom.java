package com.grm3355.zonie.commonlib.domain.chatroom.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.ColumnDefault;
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
@Table(name = "chat_rooms")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
public class ChatRoom extends BaseTimeEntity {

	// @Id
	// @GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id", insertable = false, updatable = false)
	private Long id;

	@Id
	@Column(name = "chat_room_id", unique = true, nullable = false, length = 50)
	private String chatRoomId;

	@JoinColumn(name = "festival_id", referencedColumnName = "festival_id", nullable = false)
	@ManyToOne
	private Festival festival;

	@JoinColumn(name = "user_id", referencedColumnName = "user_id", nullable = false)
	@ManyToOne
	private User user;

	@OneToMany(mappedBy = "chatRoom")
	private List<ChatRoomUser> participants;

	@Column(name = "title", nullable = false, length = 100)
	private String title;

	@Column(name = "cover_image_url", length = 100)
	private String coverImageUrl;

	@Column(name = "max_participants", nullable = false)
	private Long maxParticipants;

	@Column(name = "radius", nullable = false)
	private double radius;

	@Column(name = "position", columnDefinition = "geography(Point, 4326)")
	private Point position;

	/**
	 * 실시간 참여자 수 (RedisToDbSyncJob이 1분마다 갱신)
	 * DB에서 직접 COUNT()하는 부하를 막기 위한 비정규화 컬럼
	 */
	@ColumnDefault("0")
	@Column(name = "participant_count", nullable = false)
	private Long participantCount;

	/**
	 * 마지막 대화 시각 (RedisToDbSyncJob이 1분마다 갱신)
	 */
	@Column(name = "last_message_at")
	private LocalDateTime lastMessageAt;
}
