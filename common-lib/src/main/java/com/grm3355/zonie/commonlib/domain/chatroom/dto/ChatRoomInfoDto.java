package com.grm3355.zonie.commonlib.domain.chatroom.dto;

import java.math.BigInteger;

//import org.locationtech.jts.geom.Point;
//import org.springframework.data.geo.Point;
import org.geolatte.geom.Point;

import lombok.Getter;

@Getter
public class ChatRoomInfoDto {
	private final String chatRoomId;
	private final Long festivalId;
	private final String userId;
	private final String title;
	private final double lat;
	private final double lon;
	private final String festivalTitle;
	private final Long participantCount;

	public ChatRoomInfoDto(String chatRoomId, Long festivalId, String userId,
		String title, double lat, double lon, String festivalTitle,
		Long participantCount) {
		this.chatRoomId = chatRoomId;
		this.festivalId = festivalId;
		this.userId = userId;
		this.title = title;
		this.lat = lat;
		this.lon = lon;
		this.festivalTitle = festivalTitle;
		this.participantCount = participantCount;
	}
}
