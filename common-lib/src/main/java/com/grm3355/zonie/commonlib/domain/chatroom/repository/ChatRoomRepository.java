package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import java.time.LocalDateTime;
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
import org.springframework.transaction.annotation.Transactional;

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

	/**
	 * 1. 마지막 대화가 24시간 지난 채팅방 삭제
	 * 조건: lastMessageAt이 기준 시간보다 이전인 경우
	 * 상세: 마지막 대화가 24시간 지났거나 (대화가 있었다면), 대화가 한 번도 없는데 생성된 지 24시간이 지난 방 (대화가 없었다면) 삭제
	 */
	@Modifying
	@Transactional
	@Query("DELETE FROM ChatRoom c "
		   + "WHERE (c.lastMessageAt IS NOT NULL AND c.lastMessageAt < :cutoffTime) "
		   + "OR (c.lastMessageAt IS NULL AND c.createdAt < :cutoffTime)")
	int deleteByLastMessageAtBefore(@Param("cutoffTime") LocalDateTime cutoffTime);

	/**
	 * 2. 참여자가 0명인 채팅방 삭제
	 * 조건: 참여자가 0명 이하이고, createdAt이 유예 시간(graceTime)보다 이전인 경우
	 * 안전 장치: 생성 직후(n시간 이내)에는 0명일 수도 있으므로(방장이 입장 전),
	 * created_at이 충분히 지난 방만 삭제 대상으로 삼아야 함.
	 */
	@Modifying
	@Transactional
	@Query("DELETE FROM ChatRoom c WHERE c.participantCount <= 0 AND c.createdAt < :graceTime")
	int deleteEmptyRooms(@Param("graceTime") LocalDateTime graceTime);

	/**
	 * 3. 축제가 종료된 채팅방 삭제
	 * 조건: ChatRoom과 연결된 Festival의 eventEndDate가 오늘 이전인 경우
	 * JPQL은 연관 관계를 타고 삭제할 수 없으므로, 서브쿼리나 ID 리스트를 사용해야 함.
	 */
	@Modifying
	@Transactional
	@Query("DELETE FROM ChatRoom c WHERE c.festival.festivalId IN (SELECT f.festivalId FROM Festival f WHERE f.eventEndDate < :today)")
	int deleteByFestivalEnded(@Param("today") java.time.LocalDate today);
}
