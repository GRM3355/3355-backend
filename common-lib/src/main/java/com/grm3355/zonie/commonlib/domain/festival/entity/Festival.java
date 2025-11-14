package com.grm3355.zonie.commonlib.domain.festival.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.locationtech.jts.geom.Point;

import com.grm3355.zonie.commonlib.global.entity.BaseTimeEntity;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "festivals")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
public class Festival extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "festival_id", nullable = false, unique = true, updatable = false)
	private Long festivalId;

	@Column(name = "addr1", nullable = false, length = 1024)
	private String addr1;

	@Column(name = "content_id", nullable = false, unique = true, updatable = false)
	private int contentId;	// 공공데이터에서 제공하는 행사정보 자체 ID

	@Column(name = "event_start_date", nullable = false)
	private LocalDate eventStartDate;

	@Column(name = "event_end_date", nullable = false)
	private LocalDate eventEndDate;

	@Column(name = "first_image", length = 1024)
	private String firstImage;

	@Column(name = "first_image2", length = 1024)
	private String firstImage2;

	@Column(name = "position", columnDefinition = "geography(Point, 4326)")
	private Point position;

	@Column(name = "area_code")
	private Integer areaCode;

	@Column(name = "tel", length = 1024)
	private String tel;

	@Column(name = "title", length = 1024)
	private String title;

	@Column(name = "region", length = 20)
	private String region;

	@Column(name = "url", length = 500)
	private String url;

	@Column(name = "target_type", length = 20)
	private String targetType;

	@Column(name = "status", length = 20)
	private String status;

	@Column(name = "map_x", length = 20)
	private String mapx;

	@Column(name = "map_y", length = 20)
	private String mapy;

	@Builder.Default
	@Column(name = "chat_room_count", nullable = false)
	private int chatRoomCount = 0;

	@Builder.Default
	@Column(name = "total_participant_count")
	private Long totalParticipantCount = 0L;
}
