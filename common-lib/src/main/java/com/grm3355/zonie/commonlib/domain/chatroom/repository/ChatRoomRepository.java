package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
	Optional<ChatRoom> findByChatRoomId(String chatRoomId);

	/**
	 * 축제별 채팅 관련 JPQL
	 * festivalId로 조회
	 * @param festivalId
	 * @param keyword
	 * @param pageable
	 * @return
	 */
	@Query(
		value = """
        SELECT 
			c.chat_room_id as chatRoomId,
			c.festival_id as festivalId,			
			c.user_id as userId,
			c.title as title,
			c.position as position,
			ST_Y(c.position::geometry) AS lat,
			ST_X(c.position::geometry) AS lon,
            f.title AS festivalTitle,
            COUNT(u.user_id) AS participantCount
        FROM chat_rooms c
        LEFT JOIN festivals f ON c.festival_id = f.festival_id
        LEFT JOIN chat_room_user u ON c.chat_room_id = u.chat_room_id
        WHERE f.festival_id IS NOT NULL
          AND (:festivalId = 0 OR f.festival_id = :festivalId)
          AND (:keyword IS NULL OR c.title ILIKE CONCAT('%', :keyword, '%'))
        GROUP BY c.chat_room_id, c.festival_id, c.user_id, c.title, c.position, f.title
        """,
		countQuery = """
		SELECT count(*)
		FROM chat_rooms c
		LEFT JOIN festivals f ON c.festival_id = f.festival_id
		LEFT JOIN chat_room_user u ON c.chat_room_id = u.chat_room_id
		WHERE f.festival_id IS NOT NULL
		  AND (:festivalId = 0 OR f.festival_id = :festivalId)
		  AND (:keyword IS NULL OR c.title ILIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chat_room_id, c.festival_id, c.user_id, c.title, c.position, f.title
        """,
		nativeQuery = true
	)
	Page<ChatRoomInfoDto> chatFestivalRoomList(long festivalId, String keyword, Pageable pageable);


	/**
	 * 내 채팅 관련 JPQL(userId로 조회)
	 * @param userId
	 * @param keyword
	 * @param pageable
	 * @return
	 */
	@Query(
		value = """
        SELECT 
			c.chat_room_id as chatRoomId,
			c.festival_id as festivalId,			
			c.user_id as userId,
			c.title as title,
			c.position as position,
			ST_Y(c.position::geometry) AS lat,
			ST_X(c.position::geometry) AS lon,
            f.title AS festivalTitle,
            COUNT(u.user_id) AS participantCount
        FROM chat_rooms c
        LEFT JOIN festivals f ON c.festival_id = f.festival_id
        LEFT JOIN chat_room_user u ON c.chat_room_id = u.chat_room_id
		WHERE c.chat_room_id IS NOT NULL
		  AND (u.user_id=:userId)
          AND (:keyword IS NULL OR c.title ILIKE CONCAT('%', :keyword, '%'))
        GROUP BY c.chat_room_id, c.festival_id, c.user_id, c.title, c.position, f.title
        """,
		countQuery = """
		SELECT count(*)
        FROM chat_rooms c
        LEFT JOIN festivals f ON c.festival_id = f.festival_id
        LEFT JOIN chat_room_user u ON c.chat_room_id = u.chat_room_id
		WHERE c.chat_room_id IS NOT NULL
		  AND (u.user_id=:userId)
		  AND (:keyword IS NULL OR c.title ILIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chat_room_id, c.festival_id, c.user_id, c.title, c.position, f.title
        """,
		nativeQuery = true
	)
	Page<ChatRoomInfoDto> chatMyRoomList(String userId, String keyword, Pageable pageable);

}

