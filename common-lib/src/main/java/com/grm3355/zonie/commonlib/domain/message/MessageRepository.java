package com.grm3355.zonie.commonlib.domain.message;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {
	// (확장 기능: 과거 메시지 조회 - api-server용)
	// Page<Message> findByChatRoomIdOrderByCreatedAtDesc(String chatRoomId, Pageable pageable);
}
