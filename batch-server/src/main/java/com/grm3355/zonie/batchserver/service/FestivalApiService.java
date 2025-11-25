package com.grm3355.zonie.batchserver.service;

import java.net.URI;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FestivalApiService {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private final WebClient webClient;
	@SuppressWarnings("checkstyle:AbbreviationAsWordInName")
	private final String openapiBaseUrl = "https://apis.data.go.kr/B551011/KorService2";
	@Value("${openapi.serviceKey}")
	private String serviceKey;

	public FestivalApiService(WebClient.Builder webClientBuilder) { // WebClient는 Non-Blocking I/O 통신에 사용
		this.webClient = webClientBuilder.baseUrl(openapiBaseUrl).build();
	}

	// 오늘 날짜
	public List<ApiFestivalDto> fetchAndParseFestivals() {
		return fetchAndParseFestivals(LocalDate.now());
	}

	// 30일간
	public List<ApiFestivalDto> fetchAndParseFestivals(LocalDate startDate) {
		return fetchAndParseFestivals(startDate, startDate.plusDays(30));
	}

	// (시작, 종료) 특정 날짜를 인수로 받아 API 호출
	// Call Back URL: http://apis.data.go.kr/B551011/KorService2/searchFestival2
	public List<ApiFestivalDto> fetchAndParseFestivals(LocalDate startDate, LocalDate endDate) {
		String apiUrl = "/searchFestival2";
		String eventStartDate = startDate.format(DATE_FORMATTER);
		String eventEndDate = endDate.format(DATE_FORMATTER);
		log.info("공공데이터 API 호출: eventStartDate={}, eventEndDate={}", eventStartDate, eventEndDate);

		URI uri = UriComponentsBuilder.fromUriString(openapiBaseUrl + apiUrl)
			.queryParam("ServiceKey", serviceKey)
			.queryParam("MobileOS", "WEB")
			.queryParam("MobileApp", "zonie")
			.queryParam("_type", "json")
			.queryParam("eventStartDate", eventStartDate)
			.queryParam("eventEndDate", eventEndDate)
			.queryParam("arrange", "Q")
			.queryParam("numOfRows", "999")
			.build(true) // true -> URL 인코딩
			.toUri();

		log.info("====> 공공데이터 축제 목록 Calling URL: {}", uri);

		// WebClient를 사용한 동기 호출
		String response = webClient.get()
			.uri(uri)
			.retrieve()
			.bodyToMono(String.class)
			.block(); // WebClient 동기식으로 사용

		if (response == null) {
			log.warn("공공데이터 API 응답이 null입니다.");
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

			// items 노드가 배열일 경우 ApiFestivalDto 리스트로 변환
			if (items.isArray() && !items.isEmpty()) {
				return mapper.readerForListOf(ApiFestivalDto.class).readValue(items);
			} else if (items.isObject()) {
				// items가 1개일 때 객체로 오는 경우 방어
				return List.of(mapper.treeToValue(items, ApiFestivalDto.class));
			}

		} catch (Exception e) {
			log.error("API Response Parsing Error: {}", e.getMessage(), e);
		}
		return Collections.emptyList();
	}
}
