package com.grm3355.zonie.commonlib.domain.festival.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Repository
public interface FestivalRepository extends JpaRepository<Festival, Long> {

	Optional<Festival> findAllByContentIdAndTargetType(int contentId, String targetType);

	Festival findByContentId(int contentId);

	Optional<Festival> findByFestivalId(long festivalId);

	/**
	 * 해당축제 채팅방수 카운터
	 */
	@Modifying
	@Query("UPDATE Festival f SET f.chatRoomCount = f.chatRoomCount+1 WHERE f.festivalId = :festivalId")
	int updateFestivalChatRoomCount(Long festivalId);
	
	
	/**
	 * 채팅방 생성전에 유효한지체크(축제가 있는지, 해당날짜가 있는지)
	 * @param festivalId
	 * @return
	 */
	@Query(
		value = """
        SELECT f
        FROM Festival f
        WHERE f.festivalId = :festivalId
          AND CURRENT_TIMESTAMP Between f.eventStartDate And f.eventEndDate
      """)
	Optional<Festival> findByIsValidFestival(long festivalId);

	/**
	 * 축제 목록보기
	 */
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
			AND (f.eventEndDate >= CURRENT_TIMESTAMP AND f.eventStartDate <= CURRENT_TIMESTAMP + 30)
			AND (:keyword is null or f.title like concat('%', :keyword, '%'))
			ORDER BY
			 CASE
			   WHEN :sortType = 'DATE_ASC' THEN f.eventStartDate
			 END ASC,
			 CASE
			   WHEN :sortType = 'DATE_DESC' THEN f.eventStartDate
			 END DESC,
			 CASE
			   WHEN :sortType = 'TITLE_ASC' THEN f.title
			 END ASC,
			 CASE
			   WHEN :sortType = 'TITLE_DESC' THEN f.title
			 END DESC			
									
      """)
	Page<Festival> getFestivalList(String region, String status, String OrderType, String keyword, Pageable pageable);


}
