package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {
	Optional<ChatRoom> findByChatRoomId(String chatRoomId);


	//채팅방 참여자 오름차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON c.festival.festivalId = f.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:userId is null or c.user.userId = :userId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY f.chatRoomCount asc
      """)
	Page<ChatRoom> chatRoomList_PARTICIPANTS_ASC
	(long festivalId, String userId, String region, String keyword, Pageable pageable);

	//채팅방 참여자 내림차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON c.festival.festivalId = f.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:userId is null or c.user.userId = :userId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY f.chatRoomCount desc
      """)
	Page<ChatRoom> chatRoomList_PARTICIPANTS_DESC
	(long festivalId, String userId, String region, String keyword, Pageable pageable);

	//채팅방 참여자 등록 오름차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON c.festival.festivalId = f.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:userId is null or c.user.userId = :userId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY c.createdAt asc
      """)
	Page<ChatRoom> chatRoomList_CREATED_AT_ASC
	(long festivalId, String userId, String region, String keyword, Pageable pageable);

	//채팅방 참여자 등록 내림차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON c.festival.festivalId = f.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:userId is null or c.user.userId = :userId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY c.createdAt desc
      """)
	Page<ChatRoom> chatRoomList_CREATED_AT_DESC
	(long festivalId, String userId, String region, String keyword, Pageable pageable);

}
