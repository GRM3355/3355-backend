package com.grm3355.zonie.apiserver.domain.festival.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.apiserver.domain.chatroom.service.ChatRoomService;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalCreateRequest;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalDetailResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalResponse;
import com.grm3355.zonie.apiserver.domain.festival.dto.FestivalSearchRequest;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalOrderType;
import com.grm3355.zonie.apiserver.domain.festival.enums.FestivalStatus;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.domain.festival.entity.FestivalDetailImage;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalDetailImageRepository;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.global.enums.Region;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional
public class FestivalService {

	private final FestivalDetailImageRepository detailImageRepository;
	private final FestivalRepository festivalRepository;
	private final ChatRoomService chatRoomService;
	@Value("${chat.pre-view-day}")
	private int preview_days; //시작하기전 몇일전부터 보여주기

	public FestivalService(FestivalDetailImageRepository detailImageRepository, FestivalRepository festivalRepository,
		ChatRoomService chatRoomService) {
		this.detailImageRepository = detailImageRepository;
		this.festivalRepository = festivalRepository;
		this.chatRoomService = chatRoomService;
	}

	/**
	 * 축제목록
	 * @param req 검색dto
	 * @return page
	 */
	@Transactional
	public Page<FestivalResponse> getFestivalList(FestivalSearchRequest req) {

		//축제 위치기반 체크
		if (req.isPs()) {
			if (req.getLat() == null || req.getLon() == null || req.getRadius() == null) {
				throw new BusinessException(ErrorCode.BAD_REQUEST, "위도, 경도, 반경을 정확하게 입력하시기 바랍니다.");
			}
		}

		Sort.Order order;
		if (req.getOrder() == FestivalOrderType.DATE_ASC) {
			order = Sort.Order.asc("event_start_date");
		} else if (req.getOrder() == FestivalOrderType.DATE_DESC) {
			order = Sort.Order.desc("event_start_date");
		} else if (req.getOrder() == FestivalOrderType.TITLE_ASC) {
			order = Sort.Order.asc("title");
		} else if (req.getOrder() == FestivalOrderType.TITLE_DESC) {
			order = Sort.Order.desc("title");
		} else {
			order = Sort.Order.asc("event_start_date");
		}
		Pageable pageable = PageRequest.of(req.getPage() - 1,
			req.getPageSize(), Sort.by(order));

		//ListType 내용 가져오기
		Page<Festival> pageList = getFestivalListType(req, pageable);

		//페이지 변환
		List<FestivalResponse> dtoPage = pageList.stream().map(FestivalResponse::fromEntity)
			.collect(Collectors.toList());

		return new PageImpl<>(dtoPage, pageable, pageList.getTotalElements());
	}

	//축제별 채팅방 검색조건별 목록 가져오기
	public Page<Festival> getFestivalListType(FestivalSearchRequest req, Pageable pageable) {

		Region region = req.getRegion();
		String regionStr = region != null ? region.toString() : null;

		FestivalStatus status = req.getStatus();
		String statusStr = status != null ? status.toString() : null;

		//위치기반 검색이면
		if (req.isPs()) {
			return festivalRepository
				.getFestivalLocationBased(req.getLat(), req.getLon(), req.getRadius() * 1000.0, preview_days, pageable);
		} else {    // 전체검색이면
			return festivalRepository
				.getFestivalList(regionStr, statusStr, req.getKeyword(), preview_days, pageable);
		}
	}

	/**
	 * 축제 상세내용
	 * @param festivalId 축제 아이디
	 * @return 페스티벌 Response
	 */
	@Transactional
	public FestivalDetailResponse getFestivalContent(long festivalId) {
		Festival festival = festivalRepository.findById(festivalId)
			.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "관련 내용을 찾을 수 없습니다."));

		List<FestivalDetailImage> images =
			detailImageRepository.findByContentId(festival.getContentId());

		return FestivalDetailResponse.fromEntity(festival, images);
	}

	/**
	 * 지역분류 가져오기
	 * @return list
	 */
	public List<Map<String, String>> getRegionList() {
		return Arrays.stream(Region.values()).map(region -> Map.of(
			"region", region.getName(),
			"code", region.name()
		)).toList();
	}

	/**
	 * 지역별 축제 개수 조회
	 * (getFestivalListType의 필터 조건 중 'preview_days'를 동일하게 적용)
	 *
	 * @param region Region Enum
	 * @return 해당 지역의 축제 개수
	 */
	@Transactional(readOnly = true)
	public long getFestivalCountByRegion(Region region) {
		if (region == null) {
			throw new BusinessException(ErrorCode.BAD_REQUEST, "지역 코드를 정확하게 입력하세요. 지역코드 정보는 다음과 같습니다.\n SEOUL(\"서울\"),\n"
				+ "\tGYEONGGI(\"경기/인천\"),\n"
				+ "\tCHUNGCHEONG(\"충청/대전/세종\"),\n"
				+ "\tGANGWON(\"강원\"),\n"
				+ "\tGYEONGBUK(\"경북/대구/울산\"),\n"
				+ "\tGYEONGNAM(\"경남/부산\"),\n"
				+ "\tJEOLLA(\"전라/광주\"),\n"
				+ "\tJEJU(\"제주\")}");
		}

		// 3. Repository에 count용 메서드 호출: getFestivalList와 동일하게 preview_days를 적용하여 노출될 축제만 카운트
		return festivalRepository.countFestivalsByRegion(
			region.toString(),
			preview_days
		);
	}

	/**
	 * 축제 생성 (테스트용)
	 * @param req 생성 요청 DTO
	 * @return 생성된 페스티벌 Response
	 */
	@Transactional
	public FestivalResponse createFestival(FestivalCreateRequest req) {
		Point position;
		try {
			// PostGIS Point 객체 생성: Point(경도 위도) 형식
			String wkt = String.format("POINT(%s %s)", req.getLon(), req.getLat());

			// WKTReader는 org.locationtech.jts.io.WKTReader를 사용합니다.
			// PostgreSQL/PostGIS의 SRID 4326을 수동으로 설정합니다.
			WKTReader reader = new WKTReader();
			position = (Point) reader.read(wkt);
			position.setSRID(4326); // SRID 4326 (WGS 84) 설정

		} catch (ParseException e) {
			log.error("Position parsing error: {}", e.getMessage());
			throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR, "좌표 데이터 처리 오류");
		}

		long currentTime = System.currentTimeMillis();

		// 1. contentId: 임의의 고유값 (현재 시간의 밀리초 사용)
		int contentId = (int) (currentTime % 10000000);

		// 2. addr1: 임의의 주소
		String addr1 = "자동 생성된 임시 주소 (테스트)";

		// 3. title: 임의의 제목
		String title = "테스트 축제 #" + (currentTime % 10000);

		// 4. region: 임의의 지역 (SEOUL로 고정)
		String region = "SEOUL";

		// 5. firstImage: 임의의 이미지 URL
		String firstImage = "http://test.image.url/sample_" + contentId + ".jpg";

		// 6. eventStartDate: 오늘부터 3일 전
		LocalDate eventStartDate = LocalDate.now().minusDays(3);

		// 7. eventEndDate: 오늘부터 3일 후
		LocalDate eventEndDate = LocalDate.now().plusDays(3);

		// 8. mapx, mapy: 위경도 문자열
		String mapx = String.valueOf(req.getLon());
		String mapy = String.valueOf(req.getLat());

		// 9. status: 진행 중으로 설정
		String status = FestivalStatus.ONGOING.toString();

		// 10. areaCode: 임의의 숫자
		Integer areaCode = ThreadLocalRandom.current().nextInt(1, 30);

		// Festival 엔티티 생성
		Festival festival = Festival.builder()
			.title(title)
			.addr1(addr1)
			.contentId(contentId)
			.eventStartDate(eventStartDate)
			.eventEndDate(eventEndDate)
			.firstImage(firstImage)
			.region(region)
			.position(position)
			.mapx(mapx)
			.mapy(mapy)
			.status(status)
			.areaCode(areaCode)
			.build();

		Festival savedFestival = festivalRepository.save(festival);
		log.info("새로운 축제 생성됨: FestivalId={}, Title={}", savedFestival.getFestivalId(), savedFestival.getTitle());

		return FestivalResponse.fromEntity(savedFestival);
	}

	public void syncTotalParticipantCounts(){
		festivalRepository.syncTotalParticipantCounts();
	}
}
