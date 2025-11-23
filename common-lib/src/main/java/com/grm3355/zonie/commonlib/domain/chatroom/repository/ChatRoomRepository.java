package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

	// 종합검색 > 채팅방 검색, 종합검색에서는 festivalId가 없어야한다.
	String TOTAL_CHAT_QUERY_BASE = """
		     SELECT
		     c.chat_room_id as chatRoomId,
		     f.festival_id as festivalId,
		     c.title,
		     c.member_count as participantCount,
		     (EXTRACT(EPOCH FROM c.last_message_at) * 1000)::BIGINT AS lastMessageAt,
		     f.title AS festivalTitle,
		     ST_Y(c.position::geometry) AS lat,
		     ST_X(c.position::geometry) AS lon,
		     (EXTRACT(EPOCH FROM c.created_at) * 1000)::BIGINT AS createdAt
		     FROM chat_rooms c
		     LEFT JOIN festivals f ON f.festival_id = c.festival_id
		     WHERE (:keyword IS NULL OR c.title ILIKE :keyword)
		""";

	// 키워드가 포함된 채팅방의 개수를 세는 용도
	// 축제 테이블(f)은 개수를 세는 조건에 영향을 주지 x -> 조인하지 않음
	String TOTAL_CHAT_QUERY_BASE_COUNT = """
		   SELECT count(*)
		   FROM chat_rooms c
		   WHERE (:keyword IS NULL OR c.title ILIKE :keyword)
		""";

	// =========================================================================
	// 공통 축제별 목록 조회 쿼리, 내 채팅방 목록 조회 쿼리
	// Native Query로 LIKE 파라미터 캐스팅 문제 해결 (keyword::TEXT 사용)
	// 반환타입은 DTO Projection: 서비스 레이어에 처리
	// =========================================================================
	// 채팅방 하나는 하나의 축제에 속함 -> 중복행이 발생하지 않음
	String CHAT_QUERY_BASE = """
		     SELECT
		     c.chat_room_id as chatRoomId,
		     f.festival_id as festivalId,
		     c.title,
		     c.member_count as participantCount,
		     (EXTRACT(EPOCH FROM c.last_message_at) * 1000)::BIGINT AS lastMessageAt,
		     f.title AS festivalTitle
		,ST_Y(c.position::geometry) AS lat
		,ST_X(c.position::geometry) AS lon
		     FROM chat_rooms c
		     LEFT JOIN festivals f ON f.festival_id = c.festival_id
		     WHERE c.festival_id = :festivalId AND (:keyword IS NULL OR c.title ILIKE :keyword)
		""";
	// 키워드가 포함된 채팅방의 개수를 세는 용도
	// 축제 테이블(f)은 개수를 세는 조건에 영향을 주지 x -> 조인하지 않음
	String CHAT_QUERY_BASE_COUNT = """
		   SELECT count(*)
		   FROM chat_rooms c
		   WHERE c.festival_id = :festivalId AND (:keyword IS NULL OR c.title ILIKE :keyword)
		""";
	// chat_rooms c와 chat_room_user cru을 LEFT JOIN -> 한 채팅방에 여러 사용자가 있을 수 있음 & 다른 조인까지 함께 사용함
	// -> 중복 행 발생 또는 유효하지 않은 쿼리 가능성 -> GROUP BY로 중복 제거 보장
	// c.chat_room_id: 기준 (채팅방 자체를 고유하게)
	// c.title, c.position, c.member_count, c.last_message_at, c.created_at: 집계 함수를 사용하지 않는 모든 컬럼: 채팅방 테이블(c)에서 가져오는 속성 값
	// f.festival_id, f.title: 집계 함수를 사용하지 않는 모든 컬럼: 축제 테이블(f)에서 가져오는 속성 값
	String MY_ROOM_QUERY_BASE = """
		     SELECT
		     c.chat_room_id as chatRoomId,
		     f.festival_id as festivalId,
		     c.title as title,
		     c.member_count as participantCount,
		     (EXTRACT(EPOCH FROM c.last_message_at) * 1000)::BIGINT AS lastMessageAt,
		     f.title AS festivalTitle,
		     ST_Y(c.position::geometry) AS lat,
		     ST_X(c.position::geometry) AS lon,
		     (EXTRACT(EPOCH FROM c.created_at) * 1000)::BIGINT AS createdAt
		     FROM chat_rooms c
		  	 LEFT JOIN festivals f ON f.festival_id = c.festival_id
		     LEFT JOIN chat_room_user cru ON cru.chat_room_id = c.chat_room_id
		     WHERE c.chat_room_id IS NOT NULL AND (cru.user_id = :userId)
		"""; // Native Query에서는 GROUP BY에 DTO 필드 대신 컬럼을 명시
	// 사용자가 참여한 채팅방의 개수를 세는 용도
	// 축제 테이블(f)은 개수를 세는 조건에 영향을 주지 x -> 조인하지 않음
	String MY_ROOM_QUERY_BASE_COUNT = """
		   SELECT COUNT(DISTINCT c.chat_room_id)
		   FROM chat_rooms c
		   LEFT JOIN chat_room_user cru ON cru.chat_room_id = c.chat_room_id
		   WHERE c.chat_room_id IS NOT NULL AND (cru.user_id = :userId)
		""";

	Optional<ChatRoom> findByChatRoomId(String chatRoomId);

	/**
	 * 채팅방 ID로 조회, 비관적 락(PESSIMISTIC_WRITE)
	 * 트랜잭션 완료 시까지 다른 트랜잭션의 접근(RW)을 막아 동시성 문제 해결
	 */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT c FROM ChatRoom c WHERE c.chatRoomId = :chatRoomId")
	Optional<ChatRoom> findByChatRoomIdWithLock(@Param("chatRoomId") String chatRoomId);

	/**
	 * 종합검색에서 채팅방 검색 채팅 관련 Native Query (festivalId로 조회)
	 */
	// 종합 쿼리문
	@Query(value = TOTAL_CHAT_QUERY_BASE, countQuery = TOTAL_CHAT_QUERY_BASE_COUNT, nativeQuery = true)
	Page<ChatRoomInfoDto> totalChatFestivalRoomList(String keyword, Pageable pageable);

	/**
	 * 축제별 채팅 관련 Native Query (festivalId로 조회)
	 */
	// 종합 쿼리문
	@Query(value = CHAT_QUERY_BASE, countQuery = CHAT_QUERY_BASE_COUNT, nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList(long festivalId, String keyword, Pageable pageable);

	/**
	 * 내 채팅 관련 Native Query(userId로 조회)
	 */
	// 채팅방 종합쿼리문
	@Query(value = MY_ROOM_QUERY_BASE
				   + " GROUP BY c.chat_room_id, f.festival_id, c.title, c.position, c.member_count, c.last_message_at, f.title, c.created_at", countQuery = MY_ROOM_QUERY_BASE_COUNT, nativeQuery = true)
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

	// 마지막 대화 24시간 지난 방 ID 조회
	@Query("SELECT c.chatRoomId FROM ChatRoom c "
		   + "WHERE (c.lastMessageAt IS NOT NULL AND c.lastMessageAt < :cutoffTime) "
		   + "OR (c.lastMessageAt IS NULL AND c.createdAt < :cutoffTime)")
	List<String> findInactiveRoomIds(@Param("cutoffTime") LocalDateTime cutoffTime);

	/**
	 * 2. 참여자가 0명인 채팅방 삭제
	 * 조건: 참여자가 0명 이하이고, createdAt이 유예 시간(graceTime)보다 이전인 경우
	 * 안전 장치: 생성 직후(n시간 이내)에는 0명일 수도 있으므로(방장이 입장 전),
	 * created_at이 충분히 지난 방만 삭제 대상으로 삼아야 함.
	 */
	@Modifying
	@Transactional
	@Query(value = "DELETE FROM chat_rooms c WHERE c.member_count <= 0 AND c.created_at < :graceTime", nativeQuery = true)
	int deleteEmptyRooms(@Param("graceTime") LocalDateTime graceTime);

	// 참여자 0명인 방 ID 조회
	@Query(value = "SELECT c.chat_room_id FROM chat_rooms c WHERE c.member_count <= 0 AND c.created_at < :graceTime", nativeQuery = true)
	List<String> findEmptyRoomIds(@Param("graceTime") LocalDateTime graceTime);

	/**
	 * 3. 축제가 종료된 채팅방 삭제
	 * 조건: ChatRoom과 연결된 Festival의 eventEndDate가 오늘 이전인 경우
	 * JPQL은 연관 관계를 타고 삭제할 수 없으므로, 서브쿼리나 ID 리스트를 사용해야 함.
	 */
	@Modifying
	@Transactional
	@Query("DELETE FROM ChatRoom c WHERE c.festival.festivalId IN (SELECT f.festivalId FROM Festival f WHERE f.eventEndDate < :today)")
	int deleteByFestivalEnded(@Param("today") java.time.LocalDate today);

	// 축제 종료된 방 ID 조회 (Festival은 이미 ON DELETE CASCADE이므로, 이 로직은 ChatRoom이 Festival 삭제에 연쇄적으로 삭제될 때 사용)
	@Query("SELECT c.chatRoomId FROM ChatRoom c WHERE c.festival.eventEndDate < :today")
	List<String> findRoomsByFestivalEnded(@Param("today") LocalDate today);

	/**
	 * 특정 사용자가 참여하는 채팅방 중, 주어진 Room ID 목록에 포함되는 방의 정보를 조회
	 * ZSET 기반 정렬 후 PG에서 데이터 조회용 (roomIds: Redis에서 가져온 ID 목록 사용)
	 * ZSET 기반 복합 정렬이 이미 되어있으므로 정렬할 필요가 없지만
	 * PostgreSQL은 IN 절로 조회할 때 입력 리스트의 순서를 보장하지 않으므로, 이 순서를 강제하는 ORDER BY가 필요
	 */
	@Query(value = MY_ROOM_QUERY_BASE
				   // 1. IN 절 조건 추가 (공백 주의)
				   + " AND c.chat_room_id IN (:roomIds)"
				   // 2. GROUP BY 추가 (공백 주의)
				   + " GROUP BY c.chat_room_id, f.festival_id, c.title, c.position, c.member_count, c.last_message_at, f.title, c.created_at"
				   // 3. PostgreSQL array_position을 사용하여 순서 강제
				   //    Hibernate가 List를 PostgreSQL 배열 리터럴로 변환할 때, @Param을 사용하면 오류를 피할 수 있습니다.
				   + " ORDER BY array_position(CAST(ARRAY[:roomIds] AS TEXT[]), c.chat_room_id)",
		nativeQuery = true)
	List<ChatRoomInfoDto> chatMyRoomListByRoomIds(@Param("userId") String userId,
		@Param("roomIds") List<String> roomIds);
}
