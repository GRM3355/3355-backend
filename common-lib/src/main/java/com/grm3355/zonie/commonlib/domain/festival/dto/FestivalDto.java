package com.grm3355.zonie.commonlib.domain.festival.dto;

import java.time.LocalDate;

import org.springframework.data.geo.Point;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FestivalDto {
	private Long festivalId;
	private String addr1;
	private String addr2;
	private int contentId;
	private LocalDate eventStartDate;
	private LocalDate eventEndDate;
	private String firstImage;
	private Point position;
	private int areaCode;
	private String tel;
	private String title;
	private String region;
	private String url;
	private String targetType;
	private String status;
}
