package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.festival.entity.Festival;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
	Optional<ChatRoom> findByChatRoomId(String chatRoomId);

	/**
	 * 축제별 채팅 관련 JPQL
	 * festivalId로 조회
	 * @param festivalId
	 * @param region
	 * @param keyword
	 * @param pageable
	 * @return
	 */

	//채팅방 참여자 오름차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON f.festivalId = c.festival.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY
			 CASE
			   WHEN :sortType = 'DATE_ASC' THEN c.createdAt
			 END ASC,
			 CASE
			   WHEN :sortType = 'DATE_DESC' THEN c.createdAt
			 END DESC,
			 CASE
			   WHEN :sortType = 'PART_ASC' THEN f.chatRoomCount
			 END ASC,
			 CASE
			   WHEN :sortType = 'PART_DESC' THEN f.chatRoomCount
			 END DESC							
      """)
	Page<ChatRoom> chatFestivlRoomList
	(long festivalId, String region, String OrderType, String keyword, Pageable pageable);




	/**
	 * 내 채팅 관련 JPQL(userId로 조회)
	 * @param userId
	 * @param region
	 * @param keyword
	 * @param pageable
	 * @return
	 */
	//채팅방 참여자 오름차순 정렬x
	@Query("""
		SELECT new com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto(
			c,
			f.title,
			COUNT(u)
		)
		FROM ChatRoom c
		LEFT JOIN c.festival f
		LEFT JOIN c.participants u
		WHERE c.chatRoomId IS NOT NULL
		  AND (u.user.userId=:userId)
		  AND (:region IS NULL OR f.region = :region)
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY
		 CASE
		   WHEN :sortType = 'DATE_ASC' THEN f.eventStartDate
		 END ASC,
		 CASE
		   WHEN :sortType = 'DATE_DESC' THEN f.eventStartDate
		 END DESC,
		 CASE
		   WHEN :sortType = 'TITLE_ASC' THEN COUNT(u)
		 END ASC,
		 CASE
		   WHEN :sortType = 'TITLE_DESC' THEN COUNT(u)
		 END DESC
					 				        
    """)
	Page<ChatRoomInfoDto> chatMyRoomList
	(String userId, String region, String OrderType, String keyword, Pageable pageable);


}
