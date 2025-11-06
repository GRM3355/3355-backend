package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;
import com.grm3355.zonie.commonlib.domain.user.entity.User;

@Repository
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {
	Optional<ChatRoomUser> findByChatRoomUserId(long chatRoomId);

	Optional<ChatRoomUser> findByUserAndChatRoomId(User user, ChatRoom room); // 닉네임 중복 방지 및 재방문자 확인용

	void deleteByUserAndChatRoomId(User user, ChatRoom room); // (퇴장 시 삭제) 명시적 퇴장 (leaveRoom) 시 사용

	@Modifying
	@Query("UPDATE ChatRoomUser cru SET cru.lastReadAt = :now WHERE cru.user.userId = :userId")
	void updateLastReadAtByUserId(@Param("userId") String userId, @Param("now") LocalDateTime now); // (안 읽은 N 기능 지원) 연결 끊김 시 마지막 읽은 시각 갱신용
}
