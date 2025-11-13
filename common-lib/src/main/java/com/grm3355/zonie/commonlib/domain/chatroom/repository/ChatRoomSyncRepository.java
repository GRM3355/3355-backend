package com.grm3355.zonie.commonlib.domain.chatroom.repository;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import com.grm3355.zonie.commonlib.domain.chatroom.dto.ChatRoomSyncDto;
import com.grm3355.zonie.commonlib.domain.chatroom.entity.ChatRoom;

import lombok.Getter;

/**
 * Batch-server의 RedisToDbSyncJob에서 사용할 chat_rooms 테이블 Bulk Update 전용 Repository
 */
public interface ChatRoomSyncRepository extends Repository<ChatRoom, Long> { // ID값 타입 = String

	/**
	 * DTO 리스트를 받아 PostgreSQL의 chat_rooms 테이블을 Bulk Update 합니다.
	 * PostgreSQL의 'unnest' 함수와 'UPDATE ... FROM' 구문을 사용합니다.
	 */
	@Modifying
	@Transactional
	@Query(value = """
        UPDATE chat_rooms cr
        SET
            -- 1. 참여자 수 업데이트 (무조건 덮어쓰기)
            --    COALESCE는 data.participant_count가 null이면 cr.participant_count(기존 값)를 사용
            participant_count = COALESCE(data.participant_count, cr.participant_count),
            
            -- 2. 마지막 대화 시각 업데이트 (DB 값보다 최신일 때만)
            last_message_at = CASE
                                -- DB값이 null이거나, Redis 타임스탬프가 더 클 때
                                WHEN cr.last_message_at IS NULL OR 
                                     data.last_message_timestamp > (EXTRACT(EPOCH FROM cr.last_message_at) * 1000)
                                THEN TO_TIMESTAMP(data.last_message_timestamp / 1000.0)
                                ELSE cr.last_message_at
                            END
		FROM (
          SELECT
			UNNEST(CAST(:roomIdsArray AS TEXT[])) AS room_id,
            UNNEST(CAST(:countsArray AS BIGINT[])) AS participant_count,
            UNNEST(CAST(:timestampsArray AS BIGINT[])) AS last_message_timestamp
      ) AS data
        WHERE
            cr.chat_room_id = data.room_id;
        """, nativeQuery = true)
	void bulkUpdateChatRooms(
		@Param("roomIdsArray") String roomIdsArray,
		@Param("countsArray") String countsArray,
		@Param("timestampsArray") String timestampsArray
	);


	/**
	 * Native Query의 unnest에 List<DTO>를 직접 전달할 수 없기 때문에,
	 * 각 필드(roomId, count, timestamp)를 별도의 List로 분리하여 전달하는 래퍼(Wrapper) 클래스.
	 * (인터페이스 내부 static 중첩 클래스로 정의)
	 */
	// Getter는 @Param("#{syncData.roomIds}")에서 참조하기 위해
	@Getter
	class SyncDataWrapper {
		private final List<String> roomIds;
		private final List<Long> counts;
		private final List<Long> timestamps;

		public SyncDataWrapper(List<ChatRoomSyncDto> dtoList) {
			this.roomIds = dtoList.stream().map(ChatRoomSyncDto::roomId).toList();
			this.counts = dtoList.stream().map(ChatRoomSyncDto::participantCount).toList();
			this.timestamps = dtoList.stream().map(ChatRoomSyncDto::lastMessageTimestamp).toList();
		}
	}
}