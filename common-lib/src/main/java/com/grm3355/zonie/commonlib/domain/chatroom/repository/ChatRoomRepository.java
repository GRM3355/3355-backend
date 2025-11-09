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
         AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%'))
    """;
	String FESTIVAL_QUERY_BASE_COUNT = """
       SELECT count(*)
       FROM chat_rooms c
       LEFT JOIN festivals f ON f.festival_id = c.festival_id
       WHERE c.festival_id IS NOT NULL
         AND (:festivalId = 0 OR c.festival_id = :festivalId)
         AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%'))
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
         AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%'))
       GROUP BY c.chat_room_id, f.festival_id, c.title, c.participant_count, c.last_message_at, f.title
    """; // Native Query에서는 GROUP BY에 DTO 필드 대신 컬럼을 명시

	String MY_ROOM_QUERY_BASE_COUNT = """
       SELECT count(*)
       FROM chat_rooms c
       LEFT JOIN festivals f ON f.festival_id = c.festival_id
       LEFT JOIN chat_room_user cru ON cru.chat_room_id = c.chat_room_id
       WHERE c.chat_room_id IS NOT NULL
         AND (cru.user_id = :userId)
         AND (:keyword IS NULL OR c.title LIKE ('%' || :keyword || '%'))
       GROUP BY c.chat_room_id, f.festival_id, c.title, c.participant_count, c.last_message_at, f.title
    """; // Native Query에서는 GROUP BY에 DTO 필드 대신 컬럼을 명시


	/**
	 * 축제별 채팅 관련 Native Query (festivalId로 조회)
	 */
	//종합 쿼리문
	@Query(value = FESTIVAL_QUERY_BASE, countQuery = FESTIVAL_QUERY_BASE_COUNT, nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList
	(long festivalId, String keyword, Pageable pageable);

	/**
	 * 축제별 채팅 관련 JPQL (festivalId로 조회)
	 */
/*
	//채팅방 참여자수 오름차순 정렬
	@Query(value = FESTIVAL_QUERY_BASE + " ORDER BY c.participant_count ASC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList_PART_ASC
	(long festivalId, String keyword, Pageable pageable);

	//채팅방 참여자수 내림차순 정렬
	@Query(value = FESTIVAL_QUERY_BASE + " ORDER BY c.participant_count DESC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList_PART_DESC
	(long festivalId, String keyword, Pageable pageable);

	// 채팅방 최신순 오름차순 정렬
	@Query(value = FESTIVAL_QUERY_BASE + " ORDER BY c.created_at ASC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList_DATE_ASC
	(long festivalId, String keyword, Pageable pageable);

	// 채팅방 최신순 내림차순 정렬
	@Query(value = FESTIVAL_QUERY_BASE + " ORDER BY c.created_at DESC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList_DATE_DESC
	(long festivalId, String keyword, Pageable pageable);

	// 채팅방 활성화순 오름차순 정렬
	@Query(value = FESTIVAL_QUERY_BASE + " ORDER BY c.last_message_at ASC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList_ACTIVE_ASC
	(long festivalId, String keyword, Pageable pageable);

	// 채팅방 활성화순 내림차순 정렬
	@Query(value = FESTIVAL_QUERY_BASE + " ORDER BY c.last_message_at DESC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatFestivalRoomList_ACTIVE_DESC
	(long festivalId, String keyword, Pageable pageable);
	*/

	/**
	 * 내 채팅 관련 Native Query(userId로 조회)
	 */
	// 채팅방 종합쿼리문
	@Query(value = MY_ROOM_QUERY_BASE, countQuery = MY_ROOM_QUERY_BASE_COUNT, nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList
	(String userId, String keyword, Pageable pageable);

	/**
	 * 내 채팅 관련 JPQL(userId로 조회)
	 */
/*
	// 채팅방 참여자수 오름차순 정렬
	@Query(value = MY_ROOM_QUERY_BASE + " ORDER BY c.participant_count ASC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList_PART_ASC
	(String userId, String keyword, Pageable pageable);

	// 채팅 참여자수 내림차순 정렬
	@Query(value = MY_ROOM_QUERY_BASE + " ORDER BY c.participant_count DESC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList_PART_DESC
	(String userId, String keyword, Pageable pageable);

	// 채팅방 생성일 오름차순
	@Query(value = MY_ROOM_QUERY_BASE + " ORDER BY c.created_at ASC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList_DATE_ASC
	(String userId, String keyword, Pageable pageable);

	// 채팅방 생성일 내림차순
	@Query(value = MY_ROOM_QUERY_BASE + " ORDER BY c.created_at DESC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList_DATE_DESC
	(String userId, String keyword, Pageable pageable);

	// 채팅방 활성화순 오름차순 정렬
	@Query(value = MY_ROOM_QUERY_BASE + " ORDER BY c.last_message_at ASC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList_ACTIVE_ASC
	(String userId, String keyword, Pageable pageable);

	// 채팅방 활성화순 내림차순 정렬
	@Query(value = MY_ROOM_QUERY_BASE + " ORDER BY c.last_message_at DESC", nativeQuery = true)
	Page<ChatRoomInfoDto> chatMyRoomList_ACTIVE_DESC
	(String userId, String keyword, Pageable pageable);
	*/
}
