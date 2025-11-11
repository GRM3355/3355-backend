package com.grm3355.zonie.commonlib.domain.message.repository;

import java.time.LocalDateTime;

import org.bson.types.ObjectId;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.grm3355.zonie.commonlib.domain.message.entity.Message;

@Repository
public interface MessageRepository extends MongoRepository<Message, String> {

	/**
	 * (P1) 가장 최신 메시지 조회
	 * totalCount는 불필요하므로 Page 대신 Slice 사용
	 */
	Slice<Message> findByChatRoomIdOrderByCreatedAtDesc(String chatRoomId, Pageable pageable);
	/**
	 * (P2+) 'cursorId' (메시지 ID)보다 오래된 메시지 조회
	 * 과거 메시지 조회 - 커서 기반
	 */
	Slice<Message> findByChatRoomIdAndIdLessThanOrderByCreatedAtDesc(String chatRoomId, String cursorId, Pageable pageable);
	Slice<Message> findByChatRoomIdAndCreatedAtLessThanOrderByCreatedAtDesc(String chatRoomId, LocalDateTime createdAt, Pageable pageable);
}
