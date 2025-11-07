package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.global.enums.Region;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class FestivalResponse {

	@Schema(description = "축제 아이디", example = "1")
	private Long festivalId;

	@Schema(description = "제목", example = "강남대로 축제")
	private String title;

	@Schema(description = "주소", example = "서울 강남구 강남대로 23")
	private String addr1;

	@Schema(description = "제목", example = "2025-11-02")
	private LocalDate eventStartDate;

	@Schema(description = "제목", example = "2025-11-10")
	private LocalDate eventEndDate;

	@Schema(description = "이미지명", example = "http://tong.visitkorea.or.kr/cms/resource/76/3380276_image2_1.jpg")
	private String firstImage;

	@Schema(description = "위도", example = "(26.223)")
	private double lat;

	@Schema(description = "경도", example = "123.233")
	private double lon;

	@Schema(description = "지역명", example = "SEOUL")
	private String region;

	@Schema(description = "채팅방 갯수", example = "23")
	private int chatRoomCount;

	//임시로 주석처리
	//@Schema(description = "이미지명", example = "http://www.naver.com/aa.gif")
	//private String coverImageUrl;

	//@Schema(description = "최대참여자수", example = "500")
	//private int maxParticipants;

	//@Schema(description = "접근거리", example = "1.0")
	//private double radius;


	public static FestivalResponse fromEntity(Festival festival) {

		LocalDate startDateTime = festival.getEventStartDate();
		LocalDate endDateTime = festival.getEventEndDate();

		return new FestivalResponse(
			festival.getFestivalId(),
			festival.getTitle(),
			festival.getAddr1(),
			startDateTime,
			endDateTime,
			festival.getFirstImage(),
			festival.getPosition().getY(),
			festival.getPosition().getX(),
			festival.getRegion(),
			festival.getChatRoomCount()
		);
	}


}
