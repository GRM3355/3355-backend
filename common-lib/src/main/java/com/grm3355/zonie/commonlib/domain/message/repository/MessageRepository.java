package com.grm3355.zonie.commonlib.domain.message.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
	// (확장 기능: 과거 메시지 조회 - api-server용)
	// Page<Message> findByChatRoomIdOrderByCreatedAtDesc(String chatRoomId, Pageable pageable);
}
