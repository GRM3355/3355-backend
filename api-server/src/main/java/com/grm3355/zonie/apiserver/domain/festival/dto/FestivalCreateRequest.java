package com.grm3355.zonie.apiserver.domain.festival.dto;

import java.time.LocalDate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FestivalCreateRequest {

	@Schema(description = "제목", example = "임시 테스트 축제")
	private String title; // 필수 필드이므로 임시로 받습니다.

	@Schema(description = "주소", example = "서울 강남구 강남대로 23")
	private String addr1; // 필수 필드이므로 임시로 받습니다.

	@Schema(description = "시작일", example = "2025-12-01")
	private LocalDate eventStartDate; // 필수 필드이므로 임시로 받습니다.

	@Schema(description = "종료일", example = "2025-12-05")
	private LocalDate eventEndDate; // 필수 필드이므로 임시로 받습니다.

	@Schema(description = "이미지 URL", example = "http://tong.visitkorea.or.kr/cms/resource/sample.jpg")
	private String firstImage;

	@Schema(description = "위도", example = "37.5845")
	private double lat; // 위도 // 필수값

	@Schema(description = "경도", example = "126.9780")
	private double lon; // 경도 // 필수값

	@Schema(description = "지역명", example = "SEOUL")
	private String region;

	@Schema(description = "Content ID", example = "99999")
	private int contentId; // 필수 필드이므로 임시로 받습니다.
}
