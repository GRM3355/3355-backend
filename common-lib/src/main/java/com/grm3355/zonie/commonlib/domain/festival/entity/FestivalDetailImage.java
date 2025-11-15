package com.grm3355.zonie.commonlib.domain.festival.entity;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
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
@Table(name = "festival_detail_images")
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Getter
@Setter
public class FestivalDetailImage extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "festival_image_id", nullable = false, unique = true, updatable = false)
	private Long festivalImageId;

	@Column(name = "content_id", nullable = false, updatable = false)
	private Integer contentId; // FK, 반드시 필요

	@Column(name = "origin_img_url", nullable = false, length = 1024)
	private String originImgUrl;

	@Column(name = "img_name", nullable = false, length = 1024)
	private String imgName;

	@Column(name = "small_image_Url", nullable = false, length = 1024)
	private String smallImageUrl;

	@Column(name = "serial_num", nullable = false, length = 1024)
	private String serialNum;

}
