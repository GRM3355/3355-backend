package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	// =========================================================================
	// 공통 축제별 목록 조회 쿼리, 내 채팅방 목록 조회 쿼리
	// Native Query로 LIKE 파라미터 캐스팅 문제 해결 (keyword::TEXT 사용)
	// 반환타입은 DTO Projection: 서비스 레이어에 처리
	// =========================================================================
	String FESTIVAL_QUERY_BASE = """
		     SELECT
		     c.chat_room_id as chatRoomId,
		     f.festival_id as festivalId,
		     c.title,
		     c.participant_count as participantCount,
		     (EXTRACT(EPOCH FROM c.last_message_at) * 1000)::BIGINT AS lastMessageAt, 
		     f.title AS festivalTitle
		,ST_Y(c.position::geometry) AS lat
		,ST_X(c.position::geometry) AS lon
		     FROM chat_rooms c
		     LEFT JOIN festivals f ON f.festival_id = c.festival_id
		     WHERE c.festival_id IS NOT NULL
		       AND (:festivalId = 0 OR c.festival_id = :festivalId)
		       AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%')OR f.title LIKE ('%' || :keyword || '%') )
		""";
	String FESTIVAL_QUERY_BASE_COUNT = """
		   SELECT count(*)
		   FROM chat_rooms c
		   LEFT JOIN festivals f ON f.festival_id = c.festival_id
		   WHERE c.festival_id IS NOT NULL
		     AND (:festivalId = 0 OR c.festival_id = :festivalId)
		     AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%') OR f.title LIKE ('%' || :keyword || '%'))
		""";
	String MY_ROOM_QUERY_BASE = """
		     SELECT
		     c.chat_room_id as chatRoomId,
		     f.festival_id as festivalId, 
		     c.title, 
		     c.participant_count as participantCount, 
		     (EXTRACT(EPOCH FROM c.last_message_at) * 1000)::BIGINT AS lastMessageAt, 
		     f.title AS festivalTitle
		,ST_Y(c.position::geometry) AS lat
		,ST_X(c.position::geometry) AS lon
		     FROM chat_rooms c
		     LEFT JOIN festivals f ON f.festival_id = c.festival_id
		     LEFT JOIN chat_room_user cru ON cru.chat_room_id = c.chat_room_id
		     WHERE c.chat_room_id IS NOT NULL
		       AND (cru.user_id = :userId)
		       AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%') OR f.title LIKE ('%' || :keyword || '%') )
		     GROUP BY c.chat_room_id, f.festival_id, c.title, c.position, c.participant_count, c.last_message_at, f.title, c.created_at
		"""; // Native Query에서는 GROUP BY에 DTO 필드 대신 컬럼을 명시
	String MY_ROOM_QUERY_BASE_COUNT = """
		   SELECT COUNT(DISTINCT c.chat_room_id)
		   FROM chat_rooms c
		   LEFT JOIN festivals f ON f.festival_id = c.festival_id
		   LEFT JOIN chat_room_user cru ON cru.chat_room_id = c.chat_room_id
		   WHERE c.chat_room_id IS NOT NULL
		     AND (cru.user_id = :userId)
		     AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%') OR f.title LIKE ('%' || :keyword || '%') )
		""";

	Optional<ChatRoom> findByChatRoomId(String chatRoomId);

	/**
	 * 축제별 채팅 관련 Native Query (festivalId로 조회)
	 */
	//종합 쿼리문
	@Query(value = FESTIVAL_QUERY_BASE, countQuery = FESTIVAL_QUERY_BASE_COUNT, nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList(long festivalId, String keyword, Pageable pageable);

	/**
	 * 내 채팅 관련 Native Query(userId로 조회)
	 */
	// 채팅방 종합쿼리문
	@Query(value = MY_ROOM_QUERY_BASE, countQuery = MY_ROOM_QUERY_BASE_COUNT, nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList(String userId, String keyword, Pageable pageable);

	/**
	 * 내 채팅 관련 JPQL(userId로 조회)
	 */
	// ChatRoomRedisCleanupJob에서 사용
	List<ChatRoom> findAllByChatRoomIdIn(Collection<String> chatRoomIds);

	/**
	 * [TestManagement]
	 * 특정 축제 ID에 속한 모든 ChatRoom 엔티티 목록을 조회합니다.
	 */
	List<ChatRoom> findAllByFestivalFestivalId(Long festivalId);

	/**
	 * [TestManagement]
	 * 특정 축제 ID에 속한 ChatRoom의 개수를 조회합니다.
	 */
	long countByFestivalFestivalId(Long festivalId);

	/**
	 * [TestManagement]
	 */
	@Modifying
	@Query("DELETE FROM ChatRoom c WHERE c.chatRoomId IN :chatRoomIds")
	long deleteByChatRoomIdIn(@Param("chatRoomIds") List<String> chatRoomIds);
}
