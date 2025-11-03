package com.grm3355.zonie.apiserver.domain.batch.config;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;

import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.support.IteratorItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.transaction.PlatformTransactionManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.grm3355.zonie.apiserver.domain.batch.service.CustomSkipListener;
import com.grm3355.zonie.apiserver.domain.batch.service.MultiJsonItemReader;
import com.grm3355.zonie.commonlib.domain.batch.dto.ApiFestivalDto;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@RequiredArgsConstructor
@Profile("!test")
@EnableBatchProcessing
public class FestivalJsonBatchConfig {

	private final JobRepository jobRepository;
	private final PlatformTransactionManager transactionManager;
	private final EntityManagerFactory entityManagerFactory;
	private final ResourcePatternResolver resourcePatternResolver;
	private final FestivalRepository festivalRepository;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Bean
	public Job festivalJsonJob() {
		log.info("==============> festivalJsonJob");
		return new JobBuilder("festivalJsonJob", jobRepository)
			.start(festivalJsonStep())
			.build();
	}

	@Bean
	public Step festivalJsonStep() {
		log.info("==============> festivalJsonStep");
		return new StepBuilder("festivalJsonStep", jobRepository)
			.<ApiFestivalDto, Festival>chunk(500, transactionManager)
			.reader(multiJsonItemReader(null))
			.processor(festivalJsonProcessor())
			.writer(festivalJsonWriter())
			.faultTolerant()
			.skip(Exception.class)
			.skipLimit(Integer.MAX_VALUE)
			.listener(new CustomSkipListener())
			.build();
	}

	/**
	 * 여러 JSON 파일을 한꺼번에 읽는 ItemReader
	 */
	@Bean
	@StepScope
	public ItemReader<ApiFestivalDto> multiJsonItemReader(@Value("${batch.json.file.path}") String jsonFilePath) {
		try {
			log.info("==============> multiJsonItemReader");
			Resource[] resources = resourcePatternResolver.getResources("file:" + jsonFilePath + "/*.json");
			log.info("Resolved JSON path: {}", jsonFilePath);
			log.info("Number of JSON resources found: {}", resources.length);

			List<ItemReader<ApiFestivalDto>> readers = new ArrayList<>();
			for (Resource resource : resources) {
				readers.add(singleJsonFileReader(resource.getFile()));
			}

			// 여러 개의 ItemReader 순차적으로 실행
			//return new CompositeItemReaderBuilder<ApiFestivalDto>()
			//	.delegates(readers)
			//	.build();

			// CompositeItemReaderBuilder 대신 커스텀 MultiJsonItemReader 사용
			return new MultiJsonItemReader<>(readers);

		} catch (IOException e) {
			log.error("JSON 리소스 경로에서 파일을 로드하는 중 오류 발생: {}", jsonFilePath, e);
			throw new RuntimeException("JSON 리소스 로드 실패", e);
		}
	}

/*
	@Bean
	@StepScope
	public MultiResourceItemReader<ApiFestivalDto> multiResourceItemReader(
		@Value("${batch.json.file.path}") String csvFilePath) {
		MultiResourceItemReader<ApiFestivalDto> reader = new MultiResourceItemReader<>();
		try {
			Resource[] resources = resourcePatternResolver.getResources("file:" + csvFilePath + "/*.json");
			log.info("Resolved CSV path: {}", csvFilePath);
			log.info("Number of CSV resources found: {}", resources.length);
			reader.setResources(resources);
		} catch (IOException e) {
			log.error("CSV 리소스 경로에서 파일을 로드하는 중 오류 발생: {}", csvFilePath, e);
			throw new RuntimeException("CSV 리소스 로드 실패", e);
		}
		reader.setDelegate(festivalCsvReaderDelegate());
		return reader;
	}

	// MultiResourceItemReader를 위한 FlatFileItemReader 델리게이트
	private FlatFileItemReader<ApiFestivalDto> festivalCsvReaderDelegate() {
		FlatFileItemReader<ApiFestivalDto> flatFileItemReader = new FlatFileItemReader<>();
		flatFileItemReader.setLinesToSkip(1); // 헤더 스킵
		flatFileItemReader.setLineMapper(placeCsvLineMapper());
		return flatFileItemReader;
	}

	@Bean
	public LineMapper<PlaceCsvDto> placeCsvLineMapper() {
		DefaultLineMapper<PlaceCsvDto> defaultLineMapper = new DefaultLineMapper<>();

		DelimitedLineTokenizer delimitedLineTokenizer = new DelimitedLineTokenizer();
		delimitedLineTokenizer.setNames("placeId", "region", "placeName", "roadAddress", "lotAddress", "lat", "lng",
			"phone", "categoryName", "keyword", "categoryGroupCode", "categoryGroupName", "placeUrl");
		delimitedLineTokenizer.setDelimiter(",");
		delimitedLineTokenizer.setStrict(false); // 컬럼 수 불일치 허용

		BeanWrapperFieldSetMapper<PlaceCsvDto> beanWrapperFieldSetMapper = new BeanWrapperFieldSetMapper<>();
		beanWrapperFieldSetMapper.setTargetType(PlaceCsvDto.class);

		defaultLineMapper.setLineTokenizer(delimitedLineTokenizer);
		defaultLineMapper.setFieldSetMapper(beanWrapperFieldSetMapper);

		return defaultLineMapper;
	}

*/

	/**
	 * 개별 JSON 파일을 읽는 Reader
	 */
	private ItemReader<ApiFestivalDto> singleJsonFileReader(File file) {
		try {
			log.info("==============> singleJsonFileReader");
			JsonNode root = objectMapper.readTree(file);
			JsonNode items = root.path("response").path("body").path("items").path("item");

			List<ApiFestivalDto> list = new ArrayList<>();
			for (JsonNode node : items) {
				ApiFestivalDto dto = objectMapper.treeToValue(node, ApiFestivalDto.class);
				list.add(dto);
			}

			return new IteratorItemReader<>(list);

		} catch (Exception e) {
			log.error("JSON 파일 읽기 실패: {}", file.getAbsolutePath(), e);
			throw new RuntimeException("JSON 파일 읽기 실패", e);
		}
	}

	@Bean
	@StepScope
	public ItemProcessor<ApiFestivalDto, Festival> festivalJsonProcessor() {
		return item -> {
			try {
				log.info("==============> festivalJsonProcessor");

				//geometry 방식
				GeometryFactory geometryFactory = new GeometryFactory();
				WKTReader wktReader = new WKTReader(geometryFactory);
				Point point = null;
				if (item.getMapx() != null && item.getMapy() != null) {
					String wktPoint = String.format("POINT (%s %s)", item.getMapx(), item.getMapy());
					point = (Point)wktReader.read(wktPoint);
					point.setSRID(4326);
				}

				//geographic 방식
				org.springframework.data.geo.Point point2 = new org.springframework.data.geo.Point(
					Double.parseDouble(String.valueOf(item.getMapx())),
					Double.parseDouble(String.valueOf(item.getMapy()))); // x = 경도, y = 위도

				String targetType = "OPENAPI";
				Festival festival = Festival.builder()
					.addr1(item.getAddr1())
					.contentId(Integer.parseInt(item.getContentid()))
					.eventStartDate(parseDate(item.getEventstartdate()))
					.eventEndDate(parseDate(item.getEventenddate()))
					.firstImage(item.getFirstimage())
					.position(point)
					.areaCode(Integer.parseInt(item.getAreacode()))
					.tel(item.getTel())
					.title(item.getTitle())
					.region(getRegionCode(item.getAreacode()))
					.url(null)
					.targetType(targetType)
					.status(null)
					.build();
				return festival;

				// return Festival.builder()
				// 	.contentid(item.getContentid())
				// 	.title(item.getTitle())
				// 	.addr1(item.getAddr1())
				// 	.addr2(item.getAddr2())
				// 	.zipcode(item.getZipcode())
				// 	.tel(item.getTel())
				// 	.firstimage(item.getFirstimage())
				// 	.firstimage2(item.getFirstimage2())
				// 	.eventstartdate(item.getEventstartdate())
				// 	.eventenddate(item.getEventenddate())
				// 	.mapx(item.getMapx())
				// 	.mapy(item.getMapy())
				// 	.areacode(item.getAreacode())
				// 	.sigungucode(item.getSigungucode())
				// 	.location(point)
				// 	.build();

			} catch (Exception e) {
				log.error("JSON -> Entity 변환 중 오류 발생 (contentid: {})", item.getContentid(), e);
				return null;
			}
		};
	}

	//save
	@Bean
	public JpaItemWriter<Festival> festivalJsonWriter_old() {
		JpaItemWriter<Festival> writer = new JpaItemWriter<>();
		writer.setEntityManagerFactory(entityManagerFactory);
		return writer;
	}

	//update & save
	@Bean
	public ItemWriter<Festival> festivalJsonWriter() {
		return items -> {
			log.info("==============> festivalJsonWriter");
			for (Festival item : items) {
				Festival existing = festivalRepository.findByContentId(item.getContentId());
				if (existing != null) {
					item.setFestivalId(existing.getFestivalId());
					item.setContentId(existing.getContentId());
					item.setPosition(existing.getPosition());
					item.setUrl(existing.getUrl());
					item.setTargetType(existing.getTargetType());
					item.setStatus(existing.getStatus());
					item.setRegion(existing.getRegion());

					// 기존 데이터 업데이트
					existing.setAddr1(item.getAddr1());
					existing.setEventStartDate(item.getEventStartDate());
					existing.setEventEndDate(item.getEventEndDate());
					existing.setFirstImage(item.getFirstImage());
					existing.setAreaCode(item.getAreaCode());
					existing.setTel(item.getTel());
					existing.setTitle(item.getTitle());
					festivalRepository.save(existing);

					log.info("=====> festivalRepository update");
				} else {
					// 새 데이터 저장
					festivalRepository.save(item);
					log.info("=====> festivalRepository register");
				}
			}
		};
	}

	//시도코드를 분류별로 재코드 정의
	public String getRegionCode(String areaCode) {
		log.info("==============> getRegionCode");

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

	public LocalDate parseDate(String date) {
		log.info("==============> parseDate");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		return LocalDate.parse(date, formatter);
	}

}

