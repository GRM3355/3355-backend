package com.grm3355.zonie.batchserver.service;

import java.net.URI;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.batchserver.dto.ApiFestivalDetailImageDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.entity.FestivalDetailImage;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalDetailImageRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FestivalDetailImageApiService {

	private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
	private final WebClient webClient;
	private final String openapiBaseUrl = "https://apis.data.go.kr/B551011/KorService2/detailImage2";
	private final FestivalDetailImageRepository festivalDetailImageRepository;
	private final FestivalDetailImageBatchMapper festivalDetailImageBatchMapper;
	@Value("${openapi.serviceKey}")
	private String serviceKey;

	public FestivalDetailImageApiService(WebClient.Builder webClientBuilder,
		FestivalDetailImageRepository festivalDetailImageRepository,
		FestivalDetailImageBatchMapper festivalDetailImageBatchMapper) { // WebClient는 Non-Blocking I/O 통신에 사용
		this.webClient = webClientBuilder.baseUrl(openapiBaseUrl).build();
		this.festivalDetailImageRepository = festivalDetailImageRepository;
		this.festivalDetailImageBatchMapper = festivalDetailImageBatchMapper;
	}

	//상세이미지 저장
	public void saveFestivalDetailImages(List<Festival> festivals) {
		log.info("축제 상세 이미지 동기화 시작");

		for (Festival festival : festivals) {
			try {
				int contentId = festival.getContentId(); // 축제 contentId
				if (contentId == 0) {
					log.warn("contentId 없음 → 상세 이미지 스킵 (festivalId: {})", festival.getFestivalId());
					continue;
				}

				// 상세 이미지 API 호출
				List<ApiFestivalDetailImageDto> imageDto = fetchFestivalDetailImages(contentId);

				// DTO → Entity 변환
				List<FestivalDetailImage> imageEntities = imageDto.stream()
					//.map(festivalDetailImageBatchMapper::toDetailImageEntity)
					.map(imgDto ->
						festivalDetailImageBatchMapper.toDetailImageEntity(imgDto, festival))
					.collect(Collectors.toList());

				// 기존 이미지 삭제 후 새로 저장 (Upsert 규칙)
				festivalDetailImageRepository.deleteByFestival_ContentId(festival.getContentId());
				festivalDetailImageRepository.saveAll(imageEntities);

				log.info("상세 이미지 저장 완료 - festivalId: {}, {}건",
					festival.getFestivalId(), imageEntities.size());

			} catch (Exception e) {
				log.error("상세 이미지 저장 실패 (festivalId: {}): {}",
					festival.getFestivalId(), e.getMessage());
			}
		}

		log.info("축제 상세 이미지 동기화 완료");
	}

	/**
	 * 상세 이미지 조회
	 */
	public List<ApiFestivalDetailImageDto> fetchFestivalDetailImages(int contentId) {
		try {
			//String url = buildDetailImageApiUrl(contentId);
			//String response = restTemplate.getForObject(url, String.class);

			URI uri = UriComponentsBuilder.fromUriString(openapiBaseUrl)
				.queryParam("serviceKey", serviceKey)
				.queryParam("MobileOS", "ETC")
				.queryParam("MobileApp", "Zonie")
				.queryParam("_type", "json")
				.queryParam("contentId", contentId)
				.queryParam("imageYN", "Y")
				.queryParam("numOfRows", "10")
				.build(true) // true -> URL 인코딩
				.toUri();

			log.info("====> 공공데이터 상세이미지 목록 Calling URL: {}", uri);

			// WebClient를 사용한 동기 호출
			String response = webClient.get()
				.uri(uri)
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
