package com.grm3355.zonie.batchserver.service;

import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDetailImageDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FestivalDetailImageApiService {

	private final WebClient webClient;
	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	@Value("${openapi.serviceKey}")
	private String serviceKey;
	private final String OPENAPI_BASE_URL = "https://apis.data.go.kr/B551011/KorService2";

	public FestivalDetailImageApiService(WebClient.Builder webClientBuilder) { // WebClient는 Non-Blocking I/O 통신에 사용
		this.webClient = webClientBuilder.baseUrl(OPENAPI_BASE_URL).build();
	}

	/**
	 * 상세 이미지 조회
	 */
	public List<ApiFestivalDetailImageDto> fetchFestivalDetailImages(int contentId) {
		try {
			//String url = buildDetailImageApiUrl(contentId);
			//String response = restTemplate.getForObject(url, String.class);
			String imageUrl = "/detailImage2";

			// WebClient를 사용한 동기 호출
			String response = webClient.get()
				.uri(uriBuilder -> uriBuilder
					.path(imageUrl)
					.queryParam("serviceKey", serviceKey)
					.queryParam("MobileOS", "ETC")
					.queryParam("MobileApp", "Zonie")
					.queryParam("_type", "json")
					.queryParam("contentId", contentId)
					.queryParam("imageYN", "Y")
					.queryParam("numOfRows", "10")
					.build())
				.retrieve()
				.bodyToMono(String.class)
				.block(); // WebClient 동기식으로 사용

			if (response == null) {
				log.warn("공공데이터 이미지 API 응답이 null입니다.");
				return Collections.emptyList();
			}

			return parseDetailImageResponse(response);

		} catch (Exception e) {
			log.error("상세 이미지 조회 실패 (contentId: {}): {}", contentId, e.getMessage());
			return Collections.emptyList();
		}
	}

	private List<ApiFestivalDetailImageDto> parseDetailImageResponse(String json) {
		try {
			ObjectMapper mapper = new ObjectMapper();
			JsonNode root = mapper.readTree(json);

			JsonNode items = root.path("response")
				.path("body")
				.path("items")
				.path("item");

			if (items.isMissingNode()) {
				return Collections.emptyList();
			}

			if (items.isArray()) {
				return StreamSupport.stream(items.spliterator(), false)
					.map(node -> mapper.convertValue(node, ApiFestivalDetailImageDto.class))
					.collect(Collectors.toList());
			} else {
				ApiFestivalDetailImageDto single =
					mapper.convertValue(items, ApiFestivalDetailImageDto.class);
				return Collections.singletonList(single);
			}

		} catch (Exception e) {
			log.error("상세 이미지 JSON 파싱 실패: {}", e.getMessage());
			return Collections.emptyList();
		}
	}

}
