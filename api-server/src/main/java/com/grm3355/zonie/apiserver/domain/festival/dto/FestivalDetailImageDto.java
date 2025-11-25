package com.grm3355.zonie.apiserver.domain.festival.dto;

import com.grm3355.zonie.commonlib.domain.festival.entity.FestivalDetailImage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class FestivalDetailImageDto {
	private Long festivalImageId;
	private Integer contentId;
	private String originImgUrl;
	private String smallImageUrl;
	private String imgName;
	private String serialNum;

	public static FestivalDetailImageDto fromEntity(FestivalDetailImage entity) {
		return FestivalDetailImageDto.builder()
			.festivalImageId(entity.getFestivalImageId())
			.contentId(entity.getFestival().getContentId())  // ★ 바뀐 부분
			.originImgUrl(entity.getOriginImgUrl())
			.smallImageUrl(entity.getSmallImageUrl())
			.imgName(entity.getImgName())
			.serialNum(entity.getSerialNum())
			.build();
	}
}
