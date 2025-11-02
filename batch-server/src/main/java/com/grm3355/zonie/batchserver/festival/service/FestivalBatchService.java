package com.grm3355.zonie.batchserver.festival.service;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.domain.batch.dto.BatchDto;
import com.grm3355.zonie.commonlib.domain.batch.entity.BatchJobStatus;
import com.grm3355.zonie.commonlib.domain.batch.repository.BatchJobStatusRepository;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.CustomErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalBatchService {

	private final FestivalRepository festivalRepository;
	private final BatchJobStatusRepository batchJobStatusRepository;
	private final WebClient webClient = WebClient.create();

	@Value("${openapi.serviceKey}")
	private String serviceKey;

	public BatchDto runBatch() {
		try {
			//시작시간 저장
			LocalDateTime startTime = LocalDateTime.now();
			int totalSavedCount;

			//open api
			//관공공사에서 api 승인 허락이 나면 실행, 데이터 저장
			//int apiSavedCount = getJsonDataOpenApi(1, 0);
			//totalSavedCount =  apiSavedCount;

			// json file
			// 임시로 json 파일 실행, 데이터 저장
			int fileSavedCount = getJsonDataFile("data/festival/festivals_01.json", 0);
			int fileSavedCount2 = getJsonDataFile("data/festival/festivals_02.json", 0);
			totalSavedCount = fileSavedCount + fileSavedCount2;

			//배치파일 로그 저장
			LocalDateTime endTime = LocalDateTime.now();
			String targetType = "festival";
			BatchJobStatus batchJobStatus = BatchJobStatus.builder()
				.targetType(targetType)
				.startTime(startTime)
				.endTime(endTime)
				.totalCount(totalSavedCount)
				.build();
			BatchJobStatus savedBatch = batchJobStatusRepository.save(batchJobStatus);
			System.out.println("========>savedBatch=" + savedBatch);

			//dto로 변환
			BatchDto dto = BatchDto.builder()
				.id(savedBatch.getId())
				.targetType(savedBatch.getTargetType())
				.startTime(savedBatch.getStartTime())
				.endTime(savedBatch.getEndTime())
				.totalCount(savedBatch.getTotalCount())
				.build();
			return dto;

		} catch (Exception e) {
			log.warn("batch 처리중 오류 발생");
			e.printStackTrace();
			return null;
		}
	}

	//OPEN API  호출
	//관공공사 승인나면 호출하기로 함.
	public int getJsonDataOpenApi(int page, int savedCount) {
		try {
			// 1개월 전 ~ 1개월 후 날짜 계산
			String preDate = LocalDate.now().minusMonths(1)
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
			String nextDate = LocalDate.now().plusMonths(1)
				.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

			//int page = 1;
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

			int totalCount = root.path("response").path("body").path("totalCount").asInt();
			int totalPage = (int)Math.ceil((double)totalCount / numOfRows);

			//데이터 db에 저장
			savedCount += setFestivalSave(items);

			//마지막페이지여부체크, 아니면 다시 불러오기
			if (totalPage != page)
				getJsonDataOpenApi(page + 1, savedCount);

			log.info("총 {}건 중 {}건 저장 완료", totalCount, savedCount);
			return savedCount;

		} catch (Exception e) {
			log.warn("getJsonDataOpenApi 처리중 오류 발생");
			e.printStackTrace();
			return savedCount;
		}

	}

	//파일데이터 가져오기
	public int getJsonDataFile(String fileName, int savedCount) {
		try {
			// /resources/data/festivals.json 파일 읽기
			File file = new ClassPathResource(fileName).getFile();

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode root = objectMapper.readTree(file);
			JsonNode items = root.path("response").path("body").path("items").path("item");
			int totalCount = root.path("response").path("body").path("totalCount").asInt();

			//데이터 db에 저장
			savedCount += setFestivalSave(items);

			log.info("총 {}건 중 {}건 저장 완료", totalCount, savedCount);
			return savedCount;

		} catch (Exception e) {
			log.warn("getJsonDataFile 처리중 오류 발생");
			e.printStackTrace();
			return savedCount;
		}
	}

	public int setFestivalSave(JsonNode items) {

		int savedCount = 0;
		for (JsonNode item : items) {
			double mapx = item.path("mapx").asDouble();    //경도 : 127.7625159968
			double mapy = item.path("mapy").asDouble();    //위도 : 35.0594575822

			Point point = new Point(Double.parseDouble(String.valueOf(mapx)),
				Double.parseDouble(String.valueOf(mapy))); // x = 경도, y = 위도

			int areacode = item.path("areacode").asInt();
			int contentid = item.path("contentid").asInt();
			String targetType = "OPENAPI";

			//데이터가 db에 없없으면 등록
			Festival festival = Festival.builder()
				.addr1(item.path("addr1").asText(null))
				.addr2(item.path("addr2").asText(null))
				.contentId(contentid)
				.eventStartDate(parseDate(item.path("eventstartdate").asText()))
				.eventEndDate(parseDate(item.path("eventenddate").asText()))
				.firstImage(item.path("firstimage").asText(null))
				.position(point)
				.areaCode(areacode)
				.tel(item.path("tel").asText(null))
				.title(item.path("title").asText(null))
				.region(getRegionCode(String.valueOf(areacode)))
				.url(item.path("url").asText(null))
				.targetType(targetType)
				.status(null)
				.build();

			//contentid로 데이터 등록확인후
			Festival festival_db = festivalRepository.findAllByContentIdAndTargetType(contentid, targetType)
				.orElseThrow(() -> new BusinessException(CustomErrorCode.NOT_FOUND, "관련 정보가 없습니다."));

			//데이터가 db에 들어 있으면 업데이트
			if (festival_db != null) {
				//내용복사
				BeanUtils.copyProperties(festival, festival_db,
					"festivalId", "contentId", "targetType", "status", "createdAt");
				//수정
				festivalRepository.save(festival_db);
				log.info("Festival 수정: {}", festival);
			} else {
				festivalRepository.save(festival);
				log.info("새로운 Festival 저장: {}", festival);
			}
			savedCount++;

		}
		return savedCount;

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

}
