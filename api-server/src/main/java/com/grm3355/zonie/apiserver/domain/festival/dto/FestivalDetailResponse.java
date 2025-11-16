package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.time.LocalDate;
import java.util.List;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.entity.FestivalDetailImage;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@Setter
public class FestivalDetailResponse extends FestivalResponse {

	@Schema(description = "상세이미지", example = "")
	private List<FestivalDetailImageDto> detailImages;

	public static FestivalDetailResponse fromEntity(Festival festival,
		List<FestivalDetailImage> images) {

		// PostGIS Point가 null일 경우를 대비한 방어 코드
		Double lat = (festival.getPosition() != null) ? festival.getPosition().getY() : null;
		Double lon = (festival.getPosition() != null) ? festival.getPosition().getX() : null;

		List<FestivalDetailImageDto> imageDtos = images.stream()
			.map(FestivalDetailImageDto::fromEntity)
			.toList();

		return FestivalDetailResponse.builder()
			.festivalId(festival.getFestivalId())
			.title(festival.getTitle())
			.addr1(festival.getAddr1())
			.eventStartDate(festival.getEventStartDate())
			.eventEndDate(festival.getEventEndDate())
			.firstImage(festival.getFirstImage())
			.firstImage2(festival.getFirstImage2())
			.detailImages(imageDtos)
			.lat(lat)	// null 또는 실제 위도값
			.lon(lon)	// null 또는 실제 경도값
			.region(festival.getRegion())
			.chatRoomCount(festival.getChatRoomCount())
			.totalParticipantCount(festival.getTotalParticipantCount())
			.build();
	}
}
