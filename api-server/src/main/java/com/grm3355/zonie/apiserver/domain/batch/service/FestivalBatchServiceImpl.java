package com.grm3355.zonie.apiserver.domain.batch.service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FestivalBatchServiceImpl implements FestivalBatchService {

	private final FestivalRepository festivalRepository;
	private final WebClient webClient = WebClient.create();

	@Value("${openapi.serviceKey}")
	private String serviceKey;

	public FestivalBatchServiceImpl(FestivalRepository festivalRepository) {
		this.festivalRepository = festivalRepository;
	}

	@Override
	public void runBatch() {
		try {
			//시작시간 저장
			//LocalDateTime startTime = LocalDateTime.now();
			//int totalSavedCount;

			log.info("=======> runBatch 1");

			//open api
			//관공공사에서 api 승인 허락이 나면 실행, 데이터 저장
			//getJsonDataOpenApi(1, 0);

			// json file
			// 임시로 json 파일 실행, 데이터 저장
			getJsonDataFile("data/festival/festivals_01.json");
			getJsonDataFile("data/festival/festivals_02.json");

			//log.info("=======> runBatch totalSavedCount=" + totalSavedCount);

		} catch (Exception e) {
			log.warn("batch 처리중 오류 발생");
			e.printStackTrace();
		}
	}

	//OPEN API  호출
	//관공공사 승인나면 호출하기로 함.
	@Override
	public void getJsonDataOpenApi(int page) {
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

			//마지막페이지여부체크, 아니면 다시 불러오기
			if (totalPage != page)
				getJsonDataOpenApi(page + 1);

			log.info("총 {}건 중 {}건 저장 완료", totalCount);

		} catch (Exception e) {
			log.warn("getJsonDataOpenApi 처리중 오류 발생");
			e.printStackTrace();
		}
	}

	//파일데이터 가져오기
	@Override
	public void getJsonDataFile(String fileName) {
		try {
			// /resources/data/festivals.json 파일 읽기
			File file = new ClassPathResource(fileName).getFile();

			log.info("===> getJsonDataFile 11");
			log.info("===> getJsonDataFile 22");
			if (!file.exists()) {
				log.info("===> getJsonDataFile 33");
				new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "파일을 찾을수 없습니다.");
			}
			log.info("===> getJsonDataFile 44");

			ObjectMapper objectMapper = new ObjectMapper();
			log.info("===> getJsonDataFile 55");
			log.info("===> getJsonDataFile file=" + file);
			JsonNode root = objectMapper.readTree(file);
			log.info("===> getJsonDataFile root=" + root);

			JsonNode items = root.path("response").path("body").path("items").path("item");
			int totalCount = root.path("response").path("body").path("totalCount").asInt();

			log.info("===> getJsonDataFile 2");
			log.info("===> getJsonDataFile totalCount=" + totalCount);

			log.info("총 {}건 중 {}건 저장 완료", totalCount);

		} catch (Exception e) {
			log.warn("getJsonDataFile 처리중 오류 발생");
			e.printStackTrace();
		}
	}

	@Override
	public int setFestivalSave(JsonNode items) {

		log.info("======> setFestivalSave 1");

		try {
			int savedCount = 0;
			for (JsonNode item : items) {

				//double mapx = item.path("mapx").asDouble();    //경도 : 127.7625159968
				//double mapy = item.path("mapy").asDouble();    //위도 : 35.0594575822
				//Point point = new Point(Double.parseDouble(String.valueOf(mapx)),
				//Double.parseDouble(String.valueOf(mapy))); // x = 경도, y = 위도

				//geometry 방식
				GeometryFactory geometryFactory = new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);
				org.locationtech.jts.geom.Point point = null;
				if (item.path("mapx") != null && item.path("mapy") != null) {
					String wktPoint = String.format("POINT (%s %s)", item.path("mapx"), item.path("mapy"));
					point = (org.locationtech.jts.geom.Point)wktReader.read(wktPoint);
					point.setSRID(4326);
				}

				int areacode = item.path("areacode").asInt();
				int contentid = item.path("contentid").asInt();
				String targetType = "OPENAPI";

				log.info("======> setFestivalSave 2");

				//데이터가 db에 없없으면 등록
				Festival festival = Festival.builder()
					.addr1(item.path("addr1").asText(null))
					.contentId(contentid)
					.eventStartDate(parseDate(item.path("eventstartdate").asText()))
					.eventEndDate(parseDate(item.path("eventenddate").asText()))
					.firstImage(item.path("firstimage").asText(null))
					.position(point)
					.areaCode(areacode)
					.tel(item.path("tel").asText(null))
					.title(item.path("title").asText(null))
					.region(getRegionCode(String.valueOf(areacode)))
					.url(null)
					.targetType(targetType)
					.status(null)
					.build();

				//contentid로 데이터 등록확인후
				Festival festival_db = festivalRepository.findAllByContentIdAndTargetType(contentid, targetType)
					.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관련 정보가 없습니다."));

				log.info("======> setFestivalSave festival_db=" + festival_db);

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
		} catch (Exception e) {
			log.error("JSON -> Entity 변환 중 오류 발생");
		}
		return 0;
	}

	//시도코드를 분류별로 재코드 정의
	@Override
	public String getRegionCode(String areaCode) {

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
			regionCode = String.valueOf(Region.GYEONGGI);
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
			regionCode = String.valueOf(Region.JEOLLA);
		} else if (areaCode.equals("39")) {  //제주
			regionCode = String.valueOf(Region.JEOLLA);
		}
		return regionCode;
	}

	@Override
	public LocalDate parseDate(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return LocalDate.parse(date, formatter);
	}

}
