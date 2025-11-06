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

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {
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
			ORDER BY f.chatRoomCount asc
      """)
	Page<ChatRoom> chatFestivlRoomList_PARTICIPANTS_ASC
	(long festivalId, String region, String keyword, Pageable pageable);

	//채팅방 참여자 내림차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON f.festivalId = c.festival.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY f.chatRoomCount desc
      """)
	Page<ChatRoom> chatFestivlRoomList_PARTICIPANTS_DESC
	(long festivalId, String region, String keyword, Pageable pageable);

	//채팅방 참여자 등록 오름차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON f.festivalId = c.festival.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY c.createdAt asc
      """)
	Page<ChatRoom> chatFestivlRoomList_CREATED_AT_ASC
	(long festivalId, String region, String keyword, Pageable pageable);

	//채팅방 참여자 등록 내림차순 정렬
	@Query(
		value = """
        SELECT c
        FROM ChatRoom c LEFT JOIN Festival f ON f.festivalId = c.festival.festivalId
        WHERE c.chatRoomId is not null
			AND (:festivalId is null or :festivalId = 0 or f.festivalId = :festivalId)
			AND (:region is null or f.region = :region)
			AND (:keyword is null or c.title like concat('%', :keyword, '%'))
			ORDER BY c.createdAt desc
      """)
	Page<ChatRoom> chatFestivlRoomList_CREATED_AT_DESC
	(long festivalId, String region, String keyword, Pageable pageable);



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
        ORDER BY COUNT(u) asc
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_PARTICIPANTS_ASC(
		@Param("userId") String userId,
		@Param("region") String region,
		@Param("keyword") String keyword,
		Pageable pageable
	);


	//채팅방 참여자 내림차순 정렬
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
        ORDER BY COUNT(u) desc
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_PARTICIPANTS_DESC(
		@Param("userId") String userId,
		@Param("region") String region,
		@Param("keyword") String keyword,
		Pageable pageable
	);


	//채팅방 참여자 등일 오름차순 정렬
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
        ORDER BY c.createdAt asc
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_CREATED_AT_ASC(
		@Param("userId") String userId,
		@Param("region") String region,
		@Param("keyword") String keyword,
		Pageable pageable
	);

	//채팅방 참여자 등록일 내림차순 정렬
	@Query("""
       SELECT new com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto(
            c,
            f.title,
            COUNT(u)
        )
        FROM ChatRoom c
        LEFT JOIN c.festival f
        LEFT JOIN c.participants u 
        WHERE c.chatRoomId is not null 
          AND (:userId IS NULL OR u.user.userId = :userId)
          AND (:region IS NULL OR f.region = :region)
          AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
        GROUP BY c.chatRoomId, c.title, f.title 
        ORDER BY c.createdAt desc
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_CREATED_AT_DESC(
		@Param("userId") String userId,
		@Param("region") String region,
		@Param("keyword") String keyword,
		Pageable pageable
	);

}
