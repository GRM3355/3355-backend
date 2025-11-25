package com.grm3355.zonie.batchserver.dto;

import org.locationtech.jts.geom.Point;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class FestivalResponse {
	private Long festivalId;
	private String title;
	private String addr1;
	private String description;
	private String location;
	private String areacode;
	private String tel;
	private double latitude;   // Point.getY()
	private double longitude;  // Point.getX()

	public static FestivalResponse fromEntity(Festival festival) {
		Point point = festival.getPosition(); // JTS Point
		return FestivalResponse.builder()
			.festivalId(festival.getFestivalId())
			.title(festival.getTitle())
			.addr1(festival.getAddr1())
			.areacode(String.valueOf(festival.getAreaCode()))
			.tel(festival.getTel())
			.latitude(point != null ? point.getY() : 0)
			.longitude(point != null ? point.getX() : 0)
			.build();
	}
}
