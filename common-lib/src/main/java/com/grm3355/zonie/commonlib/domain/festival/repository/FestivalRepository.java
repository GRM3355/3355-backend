package com.grm3355.zonie.commonlib.domain.festival.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {

	Optional<Festival> findAllByContentIdAndTargetType(int contentId, String targetType);

	Festival findByContentId(int contentId);

	Optional<Festival> findByFestivalId(long festivalId);

	// 채팅방 생성전에 유효한지체크(축제가 있는지, 해당날짜가 있는지)
	// @Query(
	// 	value = """
    //     SELECT f
    //     FROM Festival f
    //     WHERE f.festivalId = :festivalId
    //       AND CURRENT_TIMESTAMP Between f.startStartDate And f.eventEndDate
    //   """)
	@Query(
		value = """
			SELECT * FROM festivals f
			WHERE f.festival_id = :festivalId 
			AND (CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum)) 
						         AND CURRENT_TIMESTAMP <= f.event_end_date)
			""",
		nativeQuery = true)
	Optional<Festival> findByIsValidFestival(long festivalId, int dayNum);

	// 이벤트 종료일이 현재 날짜보다 이전인 축제를 삭제
	void deleteByEventEndDateBefore(LocalDate date);

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
		    AND (CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum)) 
				             AND CURRENT_TIMESTAMP <= f.event_end_date)
			AND (
				:status = 'ALL' or :status is null
				OR (:status = 'ONGOING' AND f.event_start_date <= CURRENT_DATE AND f.event_end_date >= CURRENT_DATE)
				OR (:status = 'UPCOMING' AND f.event_start_date > CURRENT_DATE)
			  )
		""",
		countQuery = """
		SELECT COUNT(*)
        FROM festivals f
        WHERE f.festival_id is not null
        AND (:region is null or f.region = :region)
        	AND (:keyword is null or f.title ILIKE '%' || :keyword || '%')
		    AND (CURRENT_TIMESTAMP >= (f.event_start_date - make_interval(days => :dayNum))
				             AND CURRENT_TIMESTAMP <= f.event_end_date)
			AND (
				:status = 'ALL' or :status is null
				OR (:status = 'ONGOING' AND f.event_start_date <= CURRENT_DATE AND f.event_end_date >= CURRENT_DATE)
				OR (:status = 'UPCOMING' AND f.event_start_date > CURRENT_DATE)
			  )
		""",
		nativeQuery = true)
	Page<Festival> getFestivalList(String region, String status, String keyword,
		int dayNum, Pageable pageable);

}
