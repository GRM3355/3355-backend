package com.grm3355.zonie.batchserver.festival.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class OpenApiFestivalBatch {

	private final FestivalRepository festivalRepository;
	private final WebClient webClient = WebClient.create();

	@Value("${openapi.serviceKey}")
	private String serviceKey;

	public void runBatch() {
		try {
			// 1개월 전 ~ 1개월 후 날짜 계산
			String preDate = LocalDate.now().minusMonths(1)
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			String nextDate = LocalDate.now().plusMonths(1)
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

			int page = 1;
			int numOfRows = 200;

			String apiUrl = "https://apis.data.go.kr/B551011/KorService2/searchFestival2"
				+ "?serviceKey=" + serviceKey
				+ "&numOfRows=" + numOfRows
				+ "&pageNo=" + page
				+ "&MobileOS=ETC&MobileApp=Zonie&_type=json&arrange=C"
				+ "&eventStartDate=" + preDate
				+ "&eventEndDate=" + nextDate;

			String response = webClient.get()
				.uri(apiUrl)
				.retrieve()
				.bodyToMono(String.class)
				.block();

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode root = objectMapper.readTree(response);
			JsonNode items = root.path("response").path("body").path("items").path("item");

			for (JsonNode item : items) {
				double mapx = item.path("mapx").asDouble();
				double mapy = item.path("mapy").asDouble();

				Point point = new Point(Double.parseDouble(String.valueOf(mapx)),
					Double.parseDouble(String.valueOf(mapy))); // x = 경도, y = 위도

				int areacode = item.path("areacode").asInt();
				Festival festival = Festival.builder()
					.addr1(item.path("addr1").asText(null))
					.addr2(item.path("addr2").asText(null))
					.contentId(item.path("contentid").asInt())
					.eventStartDate(parseDate(item.path("eventstartdate").asText()))
					.eventEndDate(parseDate(item.path("eventenddate").asText()))
					.firstImage(item.path("firstimage").asText(null))
					.position(point)
					.areaCode(areacode)
					.siGunGuCode(item.path("sigungucode").asInt())
					.tel(item.path("tel").asText(null))
					.title(item.path("title").asText(null))
					.region(getRegionCode(String.valueOf(areacode)))
					.status("ACTIVE")
					.targetType("OPENAPI")
					.build();

				festivalRepository.save(festival);
			}

			//	.eventStartDate(parseDate(item.path("eventstartdate").asText()))
			//.eventEndDate(parseDate(item.path("eventenddate").asText()))

			System.out.println("Open API 축제 데이터 저장 완료");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// private LocalDate parseDate(String dateStr) {
	// 	try {
	// 		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	// 		return LocalDate.parse(dateStr, formatter).atStartOfDay();
	// 	} catch (Exception e) {
	// 		return LocalDate.now();
	// 	}
	// }

	//시도코드를 분류별로 재코드 정의
	private String getRegionCode(String areaCode) {

		// 1-서울
		// 2-인천
		// 3-대전
		// 4-대구
		// 5-광주
		// 6-부산
		// 7-울산
		// 8-세종특별자치시
		// 31-경기도
		// 32-강원특별자치도
		// 33-충청북도
		// 34-충청남도
		// 35-경상북도
		// 36-경상남도
		// 37-전북특별자치도
		// 38-전라남도
		// 39-제주특별자치도

		String regionCode = null;
		if (areaCode.equals("1")) {    //서울
			//SEOUL
			regionCode = String.valueOf(Region.SEOUL);
		} else if (areaCode.equals("2") || areaCode.equals("31")) {    //인천, 경기도
			regionCode = String.valueOf(Region.GYEONGGI_INCHEON);
		} else if (areaCode.equals("3") || areaCode.equals("8") || areaCode.equals("33") || areaCode.equals(
			"34")) { //대전, 세종, 충북, 충남
			regionCode = String.valueOf(Region.CHUNGCHEONG);
		} else if (areaCode.equals("32")) { //강원
			regionCode = String.valueOf(Region.GANGWON);
		} else if (areaCode.equals("35") || areaCode.equals("4") || areaCode.equals("7")) {  //경북, 대구, 울산
			regionCode = String.valueOf(Region.GANGWON);
		} else if (areaCode.equals("36") || areaCode.equals("6")) {  // 경남, 부산
			regionCode = String.valueOf(Region.GANGWON);
		} else if (areaCode.equals("38") || areaCode.equals("37") || areaCode.equals("5")) {  //전남, 전북, 광주
			regionCode = String.valueOf(Region.JEOLLA_GWANGJU);
		} else if (areaCode.equals("39")) {  //제주
			regionCode = String.valueOf(Region.JEOLLA_GWANGJU);
		}
		return regionCode;
	}

	private LocalDate parseDate(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return LocalDate.parse(date, formatter);
	}

}