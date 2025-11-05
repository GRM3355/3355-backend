package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.time.LocalDate;

import org.springframework.data.geo.Point;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FestivalDto {

	@Schema(description = "축제번호", example = "1")
	private Long festivalId;

	@Schema(description = "주소", example = "서울특별시 성북구 보문로 168 (삼선동5가)")
	private String addr1;

	@Schema(description = "api 콘텐츠번호", example = "2434313")
	private int contentId;

	@Schema(description = "시작일", example = "2025-11-02")
	private LocalDate eventStartDate;

	@Schema(description = "종료일", example = "2025-11-02")
	private LocalDate eventEndDate;

	@Schema(description = "이미지명", example = "http://tong.visitkorea.or.kr/cms/resource/76/3380276_image2_1.jpg")
	private String firstImage;

	@Schema(description = "지도위치(위도,경도", example = "ST_SetSRID(ST_MakePoint(127.7625159968, 35.0594575822), 4326)")
	private Point position;

	@Schema(description = "지역코드명", example = "1")
	private int areaCode;

	@Schema(description = "연락처", example = "02-6951-2012")
	private String tel;

	@Schema(description = "축제명", example = "성북 책모꼬지 북페스티벌")
	private String title;

	@Schema(description = "그룹 지역명", example = "SEOUL")
	private String region;

	@Schema(description = "홈페이지 주소명", example = "")
	private String url;

	@Schema(description = "내용구분", example = "")
	private String targetType;

	@Schema(description = "등록상태", example = "")
	private String status;
}
