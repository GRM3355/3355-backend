package com.grm3355.zonie.batchserver.service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;

@Service
public class FestivalApiService {

	private final WebClient webClient;

	// private final String OPENAPI_BASE_URL = "http://openapi.festival-api.go.kr";
	private final String OPENAPI_BASE_URL = "https://apis.data.go.kr/B551011/KorService2";

	@Value("${openapi.service-key}")
	private String serviceKey;

	public FestivalApiService(WebClient.Builder webClientBuilder) { // WebClient는 Non-Blocking I/O 통신에 사용
		this.webClient = webClientBuilder.baseUrl(OPENAPI_BASE_URL).build();
	}

	// Cron Job 실행 시 호출되는 기본 메서드 (오늘 날짜 사용)
	public List<ApiFestivalDto> fetchAndParseFestivals() {
		return fetchAndParseFestivals(LocalDate.now());
	}

	// 특정 날짜를 인수로 받아 API 호출을 수행하는 핵심 메서드
	// Call Back URL: http://apis.data.go.kr/B551011/KorService2/searchFestival2
	public List<ApiFestivalDto> fetchAndParseFestivals(LocalDate startDate) {
		String apiUrl = "/searchFestival2";
		String eventStartDate = startDate.format(DateTimeFormatter.ofPattern("yyyyMMdd")); // 주입된 startDate -> API 파라미터 생성

		// WebClient를 사용한 동기 호출
		String response = webClient.get()
			.uri(uriBuilder -> uriBuilder
				.path(apiUrl)
				.queryParam("ServiceKey", serviceKey)
				.queryParam("MobileOS", "WEB")
				.queryParam("MobileApp", "zonie")
				.queryParam("_type", "json")
				.queryParam("eventStartDate", eventStartDate) // eventStartDate 파라미터에 날짜를 사용
				.queryParam("arrange", "Q")
				.queryParam("numOfRows", "9999")
				.build())
			.retrieve()
			.bodyToMono(String.class)
			.block();

		if (response == null) {
			return Collections.emptyList();
		}

		return parseApiResponse(response);
	}
	private List<ApiFestivalDto> parseApiResponse(String response) {
		// JSON 응답 구조: { response: { body: { items: { item: [{}, {}] } } } }
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(response);

			// JSON 경로 탐색: response -> body -> items -> item
			JsonNode items = root.path("response").path("body").path("items").path("item");

			if (items.isArray()) {
				// items 노드가 배열일 경우 ApiFestivalDto 리스트로 변환
				return mapper.readerForListOf(ApiFestivalDto.class).readValue(items);
			}
		} catch (Exception e) {
			System.err.println("API Response Parsing Error: " + e.getMessage());
		}
		return Collections.emptyList();
	}
}