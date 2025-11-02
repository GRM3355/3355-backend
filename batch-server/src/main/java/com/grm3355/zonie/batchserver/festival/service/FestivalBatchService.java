package com.grm3355.zonie.batchserver.festival.service;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.festival.dto.FestivalDto;
import com.grm3355.zonie.batchserver.festival.dto.FestivalResponseWrapper;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FestivalBatchService {

	private final FestivalRepository festivalRepository;

	@Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
	public void autoFetchAndSaveFestivalData() throws Exception {
		fetchAndSaveFestivalData(1);
	}

	public JsonNode readFestivalJson() throws IOException {
		// resources/data/festivals.json 파일 로딩
		ClassPathResource resource = new ClassPathResource("data/festivals.json");
		File file = resource.getFile();

		// Jackson ObjectMapper로 JSON 파싱
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode rootNode = objectMapper.readTree(file);

		return rootNode;
	}

	public void fetchAndSaveFestivalData(int page) throws Exception {

		//String apiUrl = "https://apis.data.go.kr/B551011/KorService1/searchFestival1" +
		//	"?serviceKey=${serviceKey}" +
		//	"&numOfRows=200&pageNo=" + page + "&MobileOS=ETC&MobileApp=Zonie&_type=json";

		LocalDate now = LocalDate.now();
		LocalDate preDate = now.minusMonths(1);
		LocalDate nextDate = now.plusMonths(1);

		String apiUrl = "https://apis.data.go.kr/B551011/KorService2/searchFestival2" +
			"?serviceKey=${serviceKey}" +
			"&numOfRows=10&pageNo=" + page + "&MobileOS=ETC&MobileApp=AppTest&_type=json&arrange=C" +
			"&eventStartDate=" + preDate + "&eventEndDate=" + nextDate;

		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> response = restTemplate.getForEntity(apiUrl, String.class);

		ObjectMapper objectMapper = new ObjectMapper();
		FestivalResponseWrapper wrapper =
			objectMapper.readValue(response.getBody(), FestivalResponseWrapper.class);

		//총갯수
		int totalCount = wrapper.getResponse().getBody().getTotalCount();
		//총 페이지수
		int totalPage = (int)Math.ceil((double)totalCount / page);

		//축제목록
		List<FestivalDto> items = wrapper.getResponse().getBody().getItems().getItem();

		for (FestivalDto dto : items) {

			Point point = new Point(Double.parseDouble(dto.getMapx()),
				Double.parseDouble(dto.getMapy())); // x = 경도, y = 위도
			Festival festival = Festival.builder()
				.addr1(dto.getAddr1())
				.addr2(dto.getAddr2())
				.contentId(Integer.parseInt(dto.getContentid()))
				.eventStartDate(parseDate(dto.getEventstartdate()))
				.eventEndDate(parseDate(dto.getEventenddate()))
				.firstImage(dto.getFirstimage())
				.position(point)
				.areaCode(parseIntOrZero(dto.getAreacode()))
				.siGunGuCode(parseIntOrZero(dto.getSigungucode()))
				.tel(dto.getTel())
				.title(dto.getTitle())
				.region(getRegionCode(dto.getAreacode()))
				.status("ACTIVE")
				.targetType("OPENAPI")
				.build();
			festivalRepository.save(festival);
		}
		//마지막 페이지까지 읽기
		if (totalPage != page)
			fetchAndSaveFestivalData(page + 1);
	}

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

	private int parseIntOrZero(String val) {
		try {
			return Integer.parseInt(val);
		} catch (Exception e) {
			return 0;
		}
	}

}
