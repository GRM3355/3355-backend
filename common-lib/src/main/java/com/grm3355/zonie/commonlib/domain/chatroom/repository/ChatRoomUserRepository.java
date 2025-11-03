package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoomUser;

@Repository
public interface ChatRoomUserRepository extends JpaRepository<ChatRoomUser, Long> {
}
