package com.grm3355.zonie.batchserver.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.hibernate.type.descriptor.sql.internal.Scale6IntervalSecondDdlType;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.stereotype.Component;

import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.enums.RegionCode;

@Component
public class FestivalBatchMapper {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	String TargetType ="OPENAPI";

	// DTO를 Entity로 변환하는 public 메서드
	public Festival toEntity(ApiFestivalDto dto) {

		// 1. PostGIS Point 객체 생성
		Point geometry = createPoint(dto.getMapx(), dto.getMapy());

		// 2. 지역 코드 -> 지역명 변환
		String regionName = areaCodeChange(RegionCode.getNameByCode(dto.getAreacode()));

		return Festival.builder()
			.addr1(dto.getAddr1())
			.contentId(parseContentId(dto.getContentid()))
			.eventStartDate(LocalDate.parse(dto.getEventstartdate(), DATE_FORMATTER))
			.eventEndDate(LocalDate.parse(dto.getEventenddate(), DATE_FORMATTER))
			.firstImage(dto.getFirstimage())
			.firstImage2(dto.getFirstimage2())
			.position(geometry) // PostGIS Point 설정
			.areaCode(parseAreaCode(dto.getAreacode()))
			.tel(dto.getTel())
			.title(dto.getTitle())
			.region(regionName) // 지역명 설정
			.mapx(dto.getMapx()) // String 타입 그대로 저장
			.mapy(dto.getMapy()) // String 타입 그대로 저장
			.targetType(TargetType)
			// url, targetType, status 등 필요시 추가 매핑
			.build();
	}
	//지역명 이름을 다시 코드로 변환
	private String areaCodeChange(String areaCode){
		return Region.fromKorean(areaCode);
	}

	// 경도(X), 위도(Y)로 PostGIS Point 객체를 생성하는 private 유틸리티 메서드
	private Point createPoint(String mapx, String mapy) {
		try {
			WKTReader reader = new WKTReader();
			return (Point) reader.read("POINT (" + mapx + " " + mapy + ")");
		} catch (Exception e) {
			System.err.println("Geo Point Creation Error: mapx=" + mapx + ", mapy=" + mapy + " - " + e.getMessage());
			return null;
		}
	}

	// areacode 파싱 (NumberFormatException 방지)
	private int parseAreaCode(String areaCode) {
		try {
			return Integer.parseInt(areaCode);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	// contentId 파싱
	private int parseContentId(String contentId) {
		try {
			return Integer.parseInt(contentId);
		} catch (NumberFormatException e) {
			return 0;
		}
	}


	public Festival updateFromDto(Festival existing, ApiFestivalDto dto){

		// 1. PostGIS Point 객체 생성
		Point geometry = createPoint(dto.getMapx(), dto.getMapy());

		// 2. 지역 코드 -> 지역명 변환
		String regionName = areaCodeChange(RegionCode.getNameByCode(dto.getAreacode()));

		existing.setTitle(dto.getTitle());
		existing.setAddr1(dto.getAddr1());
		existing.setFirstImage(dto.getFirstimage());
		existing.setFirstImage2(dto.getFirstimage2());
		existing.setEventStartDate(LocalDate.parse(dto.getEventstartdate(), DATE_FORMATTER));
		existing.setEventEndDate(LocalDate.parse(dto.getEventenddate(), DATE_FORMATTER));
		existing.setPosition(geometry); // PostGIS Point 설정
		existing.setMapx(dto.getMapx()); // String 타입 그대로 저장
		existing.setMapy(dto.getMapy()); // String 타입 그대로 저장
		existing.setAreaCode(parseAreaCode(dto.getAreacode()));
		existing.setRegion(regionName);
		existing.setTel(dto.getTel());
		return existing;
	}


}