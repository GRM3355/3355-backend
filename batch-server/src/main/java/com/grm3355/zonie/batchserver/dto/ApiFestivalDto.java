package com.grm3355.zonie.batchserver.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiFestivalDto {

	//@Schema(description = "주소", example = "부산광역시 중구 광복로 72-1 (광복동2가)")
	private String addr1;

	//@Schema(description = "상세주소", example = "부산 광복로 패션거리 일대")
	private String addr2;

	//@Schema(description = "우편번호", example = "48954")
	private String zipcode;

	//@Schema(description = "대분류", example = "A02")
	private String cat1;

	//@Schema(description = "중분류", example = "A0207")
	private String cat2;

	//@Schema(description = "소분류", example = "A02070200")
	private String cat3;

	//@Schema(description = "주소", example = "부산광역시 중구 광복로 72-1 (광복동2가)")
	private String contentid;

	//@Schema(description = "콘텐츠 아이디", example = "3556962")
	private String contenttypeid;

	//@Schema(description = "등록일", example = "부산광역시 중구 광복로 72-1 (광복동2가)")
	private String createdtime;

	//@Schema(description = "축제 시작일", example = "20251101")
	private String eventstartdate;

	//@Schema(description = "죽제 종료일", example = "20251102")
	private String eventenddate;

	//@Schema(description = "이미지1", example = "http://tong.visitkorea.or.kr/cms/resource/44/3556944_image2_1.png")
	private String firstimage;

	//@Schema(description = "이미지2", example = "http://tong.visitkorea.or.kr/cms/resource/44/3556944_image3_1.png")
	private String firstimage2;

	//@Schema(description = "저작권 유형", example = "Type3")
	private String cpyrhtDivCd;

	//@Schema(description = "경도", example = "129.0327400915")
	private String mapx;

	//@Schema(description = "위도", example = "35.0986398717")
	private String mapy;

	//@Schema(description = "지도 적정레벨", example = "6")
	private String mlevel;

	//@Schema(description = "수정일", example = "20251027172640")
	private String modifiedtime;

	//@Schema(description = "시도코드", example = "6")
	private String areacode;

	//@Schema(description = "구군코드", example = "15")
	private String sigungucode;

	//@Schema(description = "연락처", example = "070-4808-2875")
	private String tel;

	//@Schema(description = "축제명", example = "부산 스트릿 페스타 인 광복")
	private String title;

	//@Schema(description = "법정동 시도 코드", example = "26")
	private String lDongRegnCd;

	//@Schema(description = "법정동 시군구 코드", example = "110")
	private String lDongSignguCd;

	//@Schema(description = "분류체계 대분류", example = "EV")
	private String lclsSystm1;

	//@Schema(description = "분류체계 중분류", example = "EV01")
	private String lclsSystm2;

	//@Schema(description = "분류체계 소분류", example = "EV010200")
	private String lclsSystm3;

	//@Schema(description = "진행상태정보", example = "")
	private String progresstype;

	//@Schema(description = "축제유형명", example = "")
	private String festivaltype;
}

