package com.grm3355.zonie.commonlib.domain.festival.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {

	Optional<Festival> findByFestivalId(long festivalId);

	// 채팅방 생성전에 유효한지체크(축제가 있는지, 해당날짜가 있는지)
	@Query(
		value = """
			SELECT * FROM festivals f
			WHERE f.festival_id = :festivalId
			AND CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
			AND CURRENT_TIMESTAMP <= (f.event_end_date + interval '1 day' - interval '1 second')
			""", nativeQuery = true)
	Optional<Festival> findByIsValidFestival(long festivalId, int dayNum);

	// 이벤트 종료일이 현재 날짜보다 이전인 축제를 목록 가져오기
	List<Festival> findByEventEndDateBefore(LocalDate date);

	// 이벤트 종료일이 현재 날짜보다 이전인 축제를 삭제
	long deleteByEventEndDateBefore(LocalDate date); // long으로 지정: JPA는 삭제된 레코드 수를 반환

	//채팅방 갯수 업데이트
	@Modifying(clearAutomatically = true)
	@Query("UPDATE Festival f SET f.chatRoomCount = f.chatRoomCount+1 WHERE f.festivalId = :festivalId")
	void updateFestivalChatRoomCount(Long festivalId);

	/**
	 * 축제 목록보기
	 */
	@Query(
		value = """
			SELECT *
			      FROM festivals f
			      WHERE f.festival_id is not null
			      AND (:region is null or f.region = :region)
			      	AND (:keyword is null OR f.title ILIKE '%' || :keyword || '%')
			    AND (
			        -- 기간 필터링 조건
			        :dayNum = -1 OR (
			            CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
			            AND CURRENT_TIMESTAMP <= (f.event_end_date + interval '1 day' - interval '1 second')
			        )
			      )
			    AND (
			        :status = 'ALL' or :status is null
			        OR (:status = 'ONGOING' AND f.event_start_date <= CURRENT_DATE AND f.event_end_date >= CURRENT_DATE)
			        OR (:status = 'UPCOMING' AND f.event_start_date > CURRENT_DATE)
			      )
			""", countQuery = """
		SELECT COUNT(*)
		      FROM festivals f
		      WHERE f.festival_id is not null
		      AND (:region is null or f.region = :region)
		      	AND (:keyword is null or f.title ILIKE '%' || :keyword || '%')
		    AND (
		        -- 기간 필터링 조건
		        :dayNum = -1 OR (
		            CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
		            AND CURRENT_TIMESTAMP <= (f.event_end_date + interval '1 day' - interval '1 second')
		        )
		      )
		    AND (
		        :status = 'ALL' or :status is null
		        OR (:status = 'ONGOING' AND f.event_start_date <= CURRENT_DATE AND f.event_end_date >= CURRENT_DATE)
		        OR (:status = 'UPCOMING' AND f.event_start_date > CURRENT_DATE)
		      )
		""",
		nativeQuery = true)
	Page<Festival> getFestivalList(String region, String status, String keyword,
		int dayNum, Pageable pageable);

	/**
	 * PostGIS의 ST_Distance 함수를 사용해 사용자의 현재 위치와 축제 위치 간의 거리를 계산합니다.
	 *
	 * @param festivalId 축제 ID
	 * @param lon 사용자 경도 (Longitude)
	 * @param lat 사용자 위도 (Latitude)
	 * @return 거리(km). 일치하는 축제가 없으면 Optional.empty()
	 */
	// 1. DB에 저장된 축제 위치 (geography 타입으로 캐스팅)
	// 2. 사용자의 현재 위치 (lon, lat)로 geography 생성
	// 3. 결과를 미터(m)에서 킬로미터(km)로 변환
	@Query(value =
		"SELECT ST_Distance("
			+ "    f.position::geography, "
			+ "    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography "
			+ ") / 1000.0 "
			+ "FROM festivals f "
			+ "WHERE f.festival_id = :festivalId",
		nativeQuery = true)
	Optional<Double> findDistanceToFestival(@Param("festivalId") long festivalId, @Param("lon") double lon,
		@Param("lat") double lat);

	/**
	 * 위치기반 축제 목록보기
	 */
	@Query(
		value = """
			SELECT *
			FROM festivals f
			WHERE f.festival_id is not null
			    AND (CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
				AND CURRENT_TIMESTAMP <= (f.event_end_date + interval '1 day' - interval '1 second'))
				AND (ST_DWithin(f.position::geography, ST_MakePoint(:lon, :lat)::geography,:radius))
			""",
		countQuery = """
			SELECT COUNT(*)
			FROM festivals f
			WHERE f.festival_id is not null
			    AND (CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
				AND CURRENT_TIMESTAMP <= (f.event_end_date + interval '1 day' - interval '1 second'))
				AND (ST_DWithin(f.position::geography, ST_MakePoint(:lon, :lat)::geography,:radius))
			""",
		nativeQuery = true)
	Page<Festival> getFestivalLocationBased(double lat, double lon, double radius,
		int dayNum, Pageable pageable);

	/**
	 * 특정 지역의 축제 개수 조회
	 */
	@Query(
		value = """
			SELECT COUNT(*)
			FROM festivals f
			WHERE f.region = :region
			AND (CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
			     AND CURRENT_TIMESTAMP <= (f.event_end_date + interval '1 day' - interval '1 second'))
			""",
		nativeQuery = true
	)
	long countFestivalsByRegion(@Param("region") String region, @Param("dayNum") int dayNum);

	/**
	 * chat_rooms 테이블의 참여자 수를 합산하여
	 * festivals 테이블의 total_participant_count 필드를 일괄 업데이트합니다.
	 */
	@Modifying
	@Transactional
	@Query(value = """
		UPDATE festivals f
		SET total_participant_count = (
		    SELECT
		        COALESCE(COUNT(DISTINCT cru.user_id), 0)
		    FROM chat_rooms cr
		    JOIN chat_room_user cru ON cr.chat_room_id = cru.chat_room_id
		    WHERE cr.festival_id = f.festival_id
		)
		WHERE f.festival_id IN (
		    SELECT cr.festival_id
		    FROM chat_rooms cr
		    WHERE cr.festival_id IS NOT NULL
		)
		""", nativeQuery = true)
	int syncTotalParticipantCounts();

	Festival findByContentId(int contentId);

	List<Festival> findByContentIdIn(List<Integer> contentIds);

	//지역별 축제갯수
	@Query(
		value = """
			SELECT f.region, COUNT(f) FROM festivals f
			WHERE 
			CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
			AND CURRENT_TIMESTAMP <= (f.event_end_date + interval '1 day' - interval '1 second')
			GROUP BY f.region
			""", nativeQuery = true)
	List<Object[]> countByRegionGroup(int dayNum);

}
