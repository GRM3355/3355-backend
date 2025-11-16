package com.grm3355.zonie.apiserver.domain.test.service;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomUserRepository;
import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;
import com.grm3355.zonie.commonlib.domain.message.entity.Message;
import com.grm3355.zonie.commonlib.domain.message.repository.MessageRepository;
import com.grm3355.zonie.commonlib.global.exception.BusinessException;
import com.grm3355.zonie.commonlib.global.exception.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Profile("!prod")
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class TestManagementService {

	private final FestivalRepository festivalRepository;
	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomUserRepository chatRoomUserRepository;
	private final MessageRepository messageRepository;
	private final StringRedisTemplate stringRedisTemplate;

	/**
	 * 축제 삭제 (채팅방 존재 시 에러)
	 */
	public void deleteFestivalSafe(long festivalId) {
		if (!festivalRepository.existsById(festivalId)) {
			return; // 이미 없으면 성공
		}

		long chatRoomCount = chatRoomRepository.countByFestivalFestivalId(festivalId);
		if (chatRoomCount > 0) {
			throw new BusinessException(ErrorCode.BAD_REQUEST,
				"축제를 삭제할 수 없습니다. " + chatRoomCount + "개의 채팅방이 존재합니다. (mode=CASCADE를 사용하세요)");
		}

		festivalRepository.deleteById(festivalId);
		log.warn("[TEST-MGMT] Festival(ID: {})이(가) 안전하게 삭제되었습니다.", festivalId);
	}

	/**
	 * 축제 및 관련 모든 데이터 삭제 (cascade)
	 */
	public void deleteFestivalCascade(long festivalId) {
		if (!festivalRepository.existsById(festivalId)) {
			return; // 이미 없으면 성공
		}

		// 1. 이 축제에 속한 모든 채팅방 ID 조회
		List<ChatRoom> chatRooms = chatRoomRepository.findAllByFestivalFestivalId(festivalId);
		if (!chatRooms.isEmpty()) {
			List<String> roomIds = chatRooms.stream()
				.map(ChatRoom::getChatRoomId)
				.toList();

			// 2. 채팅방 삭제 로직 재사용
			deleteChatRoomCascade(roomIds);
		}

		// 3. 축제 삭제
		festivalRepository.deleteById(festivalId);
		log.warn("[TEST-MGMT] Festival(ID: {}) 및 하위 데이터 (채팅방 {}개)가 연쇄 삭제되었습니다.", festivalId, chatRooms.size());
	}

	/**
	 * 채팅방 및 관련 모든 데이터 삭제 (cascade)
	 */
	public void deleteChatRoomCascade(String chatRoomId) {
		deleteChatRoomCascade(List.of(chatRoomId));
	}

	/**
	 * 여러 채팅방 및 관련 모든 데이터 삭제 (cascade)
	 * 삭제 순서:
	 * 1. MongoDB (조회): Message ID 목록을 미리 조회
	 * 2. Redis (삭제): 조회된 Message ID와 ChatRoom ID를 사용해 모든 관련 키(좋아요, 참여자 수 등) 삭제
	 * 3. MongoDB (삭제): Message 도큐먼트 삭제
	 * 4. JPA (삭제): ChatRoomUser 삭제 (FK -> ChatRoom을 참조)
	 * 5. JPA (삭제): ChatRoom 삭제 (FK -> Festival)
	 */
	public void deleteChatRoomCascade(List<String> chatRoomIds) {
		if (chatRoomIds == null || chatRoomIds.isEmpty()) {
			return;
		}

		// 1. (Mongo) 삭제할 Message ID 목록 조회
		List<String> messageIds = messageRepository.findAllByChatRoomIdIn(chatRoomIds)
			.stream()
			.map(Message::getId)
			.toList();
		log.warn("[TEST-MGMT] Found {} message IDs to delete from rooms: {}", messageIds.size(), chatRoomIds);

		// 2. (Redis) 좋아요 관련 키 삭제
		if (!messageIds.isEmpty()) {
			List<String> likeKeys = messageIds.stream()
				.flatMap(msgId -> Stream.of(
					"message:liked_by:" + msgId,
					"message:like_count:" + msgId
				))
				.toList();

			Long deletedLikeKeys = stringRedisTemplate.delete(likeKeys);
			log.warn("[TEST-MGMT] Redis: Deleted {} 'like' keys.", deletedLikeKeys);
		}

		// 3. (Redis) 채팅방 관련 키 삭제
		List<String> chatRoomKeys = chatRoomIds.stream()
			.flatMap(roomId -> Stream.of(
				"chatroom:participants:" + roomId,
				"chatroom:nickname_seq:" + roomId,
				"chatroom:last_msg_at:" + roomId,
				"chatroom:last_msg_content:" + roomId
			))
			.toList();

		Long deletedChatKeys = stringRedisTemplate.delete(chatRoomKeys);
		log.warn("[TEST-MGMT] Redis: Deleted {} 'chatroom' keys.", deletedChatKeys);
		// (cf. 'user:rooms:' 키는 유저 ID 기준이라 이 로직에서는 삭제하지 x.)

		// 4. (Mongo) Message 도큐먼트 삭제
		long deletedMsgs = messageRepository.deleteByChatRoomIdIn(chatRoomIds);
		log.warn("[TEST-MGMT] MongoDB(messages) deleted {} documents.", deletedMsgs);

		// 5. (JPA) ChatRoomUser 엔티티 삭제
		long deletedUsers = chatRoomUserRepository.deleteByChatRoom_ChatRoomIdIn(chatRoomIds);
		log.warn("[TEST-MGMT] JPA(chat_room_user) deleted {} participants.", deletedUsers);

		// 6. JPA ChatRoom 엔티티 삭제
		long deletedRooms = chatRoomRepository.deleteByChatRoomIdIn(chatRoomIds);
		log.warn("[TEST-MGMT] JPA(chat_rooms) deleted {} rooms.", deletedRooms);
	}

	/**
	 * Redis 채팅/좋아요 관련 키 강제 삭제
	 * (* 주의: 이 메서드는 'keys' 명령어를 사용하므로 Prod 프로필에서 절대 실행되면 안 됨.) (추후 제거)
	 */
	public long flushChatKeysFromRedis() {
		log.warn("[TEST-MGMT] Redis 'message:*' 및 'chatroom:*' 키 삭제 작업을 시작합니다...");

		// 1. 삭제할 키 패턴 정의 (세션/위치 키는 제외)
		Set<String> messageKeys = stringRedisTemplate.keys("message:*");
		Set<String> chatroomKeys = stringRedisTemplate.keys("chatroom:*");

		long deletedCount = 0;

		if (!messageKeys.isEmpty()) {
			log.info("[TEST-MGMT] 'message:*' 패턴 키 {}개를 삭제합니다.", messageKeys.size());
			deletedCount += stringRedisTemplate.delete(messageKeys);
		}

		if (!chatroomKeys.isEmpty()) {
			log.info("[TEST-MGMT] 'chatroom:*' 패턴 키 {}개를 삭제합니다.", chatroomKeys.size());
			deletedCount += stringRedisTemplate.delete(chatroomKeys);
		}

		log.warn("[TEST-MGMT] 총 {}개의 Redis 키가 삭제되었습니다.", deletedCount);
		return deletedCount;
	}
}
