package com.grm3355.zonie.commonlib.domain.festival.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;
import com.grm3355.zonie.commonlib.global.enums.Region;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {


	Optional<Festival> findAllByContentIdAndTargetType(int contentId, String targetType);

	Festival findByContentId(int contentId);

	Optional<Festival> findByFestivalId(long festivalId);

	//채팅방 생성전에 유효한지체크(축제가 있는지, 해당날짜가 있는지)
	@Query(
		value = """
        SELECT f
        FROM Festival f
        WHERE f.festivalId = :festivalId
          AND CURRENT_TIMESTAMP Between f.eventStartDate And f.eventEndDate
      """)
	Optional<Festival> findByIsValidFestival(long festivalId);

	// 이벤트 종료일이 현재 날짜보다 이전인 축제를 삭제
	void deleteByEventEndDateBefore(LocalDate date);


	/**
	 * 축제 목록보기
	 */
	//축제목록 등록일 오름차순 정렬
	@Query(
		value = """
        SELECT f
        FROM Festival f 
        WHERE f.festivalId is not null
			AND (:region is null or f.region = :region)
        	AND (:keyword is null or f.title like concat('%', :keyword, '%'))
			AND (f.eventEndDate >= CURRENT_TIMESTAMP AND f.eventStartDate <= :endDateLimit)
			AND (
				:status = 'ALL' or :status is null
				OR (:status = 'ONGOING' AND f.eventStartDate <= CURRENT_DATE AND f.eventEndDate >= CURRENT_DATE)
				OR (:status = 'UPCOMING' AND f.eventStartDate > CURRENT_DATE)
			  )
			ORDER BY f.eventStartDate ASC
      """)
	Page<Festival> getFestivalList_DATE_ASC(String region, String status, String keyword,
		LocalDate endDateLimit, Pageable pageable);

	//축제목록 등록일 내림차순 정렬
	@Query(
		value = """
        SELECT f
        FROM Festival f 
        WHERE f.festivalId is not null
			AND (:region is null or f.region = :region)
			AND (
				:status = 'ALL'
				OR (:status = 'ONGOING' AND f.eventStartDate <= CURRENT_DATE AND f.eventEndDate >= CURRENT_DATE)
				OR (:status = 'UPCOMING' AND f.eventStartDate > CURRENT_DATE)
			  )		
			AND (f.eventEndDate >= CURRENT_TIMESTAMP AND f.eventStartDate <= :endDateLimit)
			AND (:keyword is null or f.title like concat('%', :keyword, '%'))
			ORDER BY f.eventStartDate DESC
      """)
	Page<Festival> getFestivalList_DATE_DESC(String region, String status, String keyword,
		LocalDate endDateLimit, Pageable pageable);

	//축제목록 개최일 오름차순 정렬
	@Query(
		value = """
        SELECT f
        FROM Festival f 
        WHERE f.festivalId is not null
			AND (:region is null or f.region = :region)
			AND (
				:status = 'ALL'
				OR (:status = 'ONGOING' AND f.eventStartDate <= CURRENT_DATE AND f.eventEndDate >= CURRENT_DATE)
				OR (:status = 'UPCOMING' AND f.eventStartDate > CURRENT_DATE)
			  )		
			AND (f.eventEndDate >= CURRENT_TIMESTAMP AND f.eventStartDate <= :endDateLimit)
			AND (:keyword is null or f.title like concat('%', :keyword, '%'))
			ORDER BY f.title ASC
      """)
	Page<Festival> getFestivalList_TITLE_ASC(String region, String status, String keyword,
		LocalDate endDateLimit, Pageable pageable);

	//축제목록 개최일 내림차순 정렬
	@Query(
		value = """
        SELECT f
        FROM Festival f 
        WHERE f.festivalId is not null
			AND (:region is null or f.region = :region)
			AND (
				:status = 'ALL'
				OR (:status = 'ONGOING' AND f.eventStartDate <= CURRENT_DATE AND f.eventEndDate >= CURRENT_DATE)
				OR (:status = 'UPCOMING' AND f.eventStartDate > CURRENT_DATE)
			  )		
			AND (f.eventEndDate >= CURRENT_TIMESTAMP AND f.eventStartDate <= :endDateLimit)
			AND (:keyword is null or f.title like concat('%', :keyword, '%'))
			ORDER BY f.title DESC
      """)
	Page<Festival> getFestivalList_TITLE_DESC(String region, String status, String keyword,
		LocalDate endDateLimit, Pageable pageable);


}
