package com.grm3355.zonie.batchserver.festival.service;

import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Point;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class LocalJsonFestivalBatch {

	private final FestivalRepository festivalRepository;

	public void runBatch() {
		try {
			// /resources/data/festivals.json 파일 읽기
			File file = new ClassPathResource("data/festival/festivals.json").getFile();

			ObjectMapper objectMapper = new ObjectMapper();
			JsonNode root = objectMapper.readTree(file);
			JsonNode items = root.path("response").path("body").path("items").path("item");

			for (JsonNode item : items) {
				double mapx = item.path("mapx").asDouble();
				double mapy = item.path("mapy").asDouble();

				Point point = new Point(Double.parseDouble(String.valueOf(mapx)),
					Double.parseDouble(String.valueOf(mapy))); // x = 경도, y = 위도

				Festival festival = Festival.builder()
					.addr1(item.path("addr1").asText(null))
					.addr2(item.path("addr2").asText(null))
					.contentId(item.path("contentid").asInt())
					.eventStartDate(parseDate(item.path("eventstartdate").asText()))
					.eventEndDate(parseDate(item.path("eventenddate").asText()))
					.firstImage(item.path("firstimage").asText(null))
					.position(point)
					.areaCode(item.path("areacode").asInt())
					.siGunGuCode(item.path("sigungucode").asInt())
					.tel(item.path("tel").asText(null))
					.title(item.path("title").asText(null))
					.region(null)
					.status("ACTIVE")
					.targetType("LOCAL_JSON")
					.build();

				festivalRepository.save(festival);
			}

			System.out.println("로컬 JSON 축제 데이터 저장 완료");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private LocalDate parseDate(String date) {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return LocalDate.parse(date, formatter);
	}

}
