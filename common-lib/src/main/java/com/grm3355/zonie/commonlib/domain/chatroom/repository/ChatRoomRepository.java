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
import com.grm3355.zonie.commonlib.global.enums.Region;

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
	//채팅방 참여자수 오름차순 정렬
	@Query("""
		SELECT new com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto(
			c,
			f.title,
			COUNT(u)
		)
		FROM ChatRoom c
		LEFT JOIN c.festival f
		LEFT JOIN c.participants u
		WHERE f.festivalId is not null
		  AND (:festivalId =0 or f.festivalId =: festivalId)
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY COUNT(u) ASC
    """)
	Page<ChatRoomInfoDto> chatFestivalRoomList_PART_ASC
	(long festivalId, String keyword, Pageable pageable);

	//채팅방 참여자수 내림차순 정렬
	@Query(
		value = """
		SELECT new com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto(
			c,
			f.title,
			COUNT(u)
		)
		FROM ChatRoom c
		LEFT JOIN c.festival f
		LEFT JOIN c.participants u
		WHERE f.festivalId is not null
		  AND (:festivalId =0 or f.festivalId =: festivalId)
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY COUNT(u) DESC
      """)
	Page<ChatRoomInfoDto> chatFestivalRoomList_PART_DESC
		(long festivalId, String keyword, Pageable pageable);

	//채팅방 최신순 오름차순 정렬
	@Query(
		value = """
		SELECT new com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto(
			c,
			f.title,
			COUNT(u)
		)
		FROM ChatRoom c
		LEFT JOIN c.festival f
		LEFT JOIN c.participants u
		WHERE f.festivalId is not null
		  AND (:festivalId =0 or f.festivalId =: festivalId)
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY c.createdAt ASC
      """)
	Page<ChatRoomInfoDto> chatFestivalRoomList_DATE_ASC
		(long festivalId, String keyword, Pageable pageable);

	//채팅방 최신순 내림차순 정렬
	@Query(
		value = """
		SELECT new com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomInfoDto(
			c,
			f.title,
			COUNT(u)
		)
		FROM ChatRoom c
		LEFT JOIN c.festival f
		LEFT JOIN c.participants u
		WHERE f.festivalId is not null
		  AND (:festivalId =0 or f.festivalId =: festivalId)
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY c.createdAt DESC
      """)
	Page<ChatRoomInfoDto> chatFestivalRoomList_DATE_DESC
		(long festivalId, String keyword, Pageable pageable);

	/**
	 * 내 채팅 관련 JPQL(userId로 조회)
	 * @param userId
	 * @param keyword
	 * @param pageable
	 * @return
	 */
	//채팅방 참여자수 오름차순 정렬
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
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY COUNT(u) ASC
					 				        
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_PART_ASC
	(String userId, String keyword, Pageable pageable);

	//채팅 참여자수 내림차순 정렬
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
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY COUNT(u) DESC
					 				        
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_PART_DESC
		(String userId, String keyword, Pageable pageable);

	//등록일 오름차순
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
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY c.createdAt ASC
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_DATE_ASC
		(String userId, String keyword, Pageable pageable);

	//등록일 내림차순
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
		  AND (:keyword IS NULL OR c.title LIKE CONCAT('%', :keyword, '%'))
		GROUP BY c.chatRoomId, c.title, f.title
		ORDER BY c.createdAt DESC
    """)
	Page<ChatRoomInfoDto> chatMyRoomList_DATE_DESC
		(String userId, String keyword, Pageable pageable);
}
