package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.time.LocalDate;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

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

	@Schema(description = "시작일", example = "2025-11-02")
	private LocalDate eventStartDate;

	@Schema(description = "종료일", example = "2025-11-10")
	private LocalDate eventEndDate;

	@Schema(description = "이미지명", example = "http://tong.visitkorea.or.kr/cms/resource/76/3380276_image2_1.jpg")
	private String firstImage;

	@Schema(description = "이미지명2", example = "http://tong.visitkorea.or.kr/cms/resource/76/3380276_image2_2.jpg")
	private String firstImage2;

	@Schema(description = "위도", example = "26.223")
	private Double lat;

	@Schema(description = "경도", example = "123.233")
	private Double lon;

	@Schema(description = "지역명", example = "SEOUL")
	private String region;

	@Schema(description = "채팅방 갯수", example = "23")
	private int chatRoomCount;

	@Schema(description = "축제 총 참여자 수", example = "300")
	private Long totalParticipantCount;


	public static FestivalResponse fromEntity(Festival festival) {

		// PostGIS Point가 null일 경우를 대비한 방어 코드
		Double lat = (festival.getPosition() != null) ? festival.getPosition().getY() : null;
		Double lon = (festival.getPosition() != null) ? festival.getPosition().getX() : null;

		return FestivalResponse.builder()
			.festivalId(festival.getFestivalId())
			.title(festival.getTitle())
			.addr1(festival.getAddr1())
			.eventStartDate(festival.getEventStartDate())
			.eventEndDate(festival.getEventEndDate())
			.firstImage(festival.getFirstImage())
			.lat(lat)	// null 또는 실제 위도값
			.lon(lon)	// null 또는 실제 경도값
			.region(festival.getRegion())
			.chatRoomCount(festival.getChatRoomCount())
			.totalParticipantCount(festival.getTotalParticipantCount())
			.build();
	}
}
