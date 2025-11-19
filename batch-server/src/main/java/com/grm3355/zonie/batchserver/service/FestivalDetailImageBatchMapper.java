package com.grm3355.zonie.batchserver.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

import com.grm3355.zonie.batchserver.dto.ApiFestivalDetailImageDto;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.entity.FestivalDetailImage;
import com.grm3355.zonie.commonlib.global.enums.RegionCode;

@Component
public class FestivalDetailImageBatchMapper {

	// DTO를 Entity로 변환하는 public 메서드
	public FestivalDetailImage toDetailImageEntity(ApiFestivalDetailImageDto dto, Festival festival) {

		return FestivalDetailImage.builder()
			//.contentId(parseContentId(dto.getContentid()))
			.festival(festival)
			.originImgUrl(dto.getOriginimgurl() != null ? dto.getOriginimgurl() : dto.getSmallimageurl())
			.imgName(dto.getImgname())
			.smallImageUrl(dto.getSmallimageurl())
			.serialNum(dto.getSerialnum())
			.build();
	}

	// contentId 파싱
	private int parseContentId(String contentId) {
		try {
			return Integer.parseInt(contentId);
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}