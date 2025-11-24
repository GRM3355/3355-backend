package com.grm3355.zonie.batchserver.job;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.batchserver.service.ChatRoomCleanupService;
import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomDeletionJob {

	private final ChatRoomRepository chatRoomRepository;
	private final ChatRoomCleanupService chatRoomCleanupService;

	/**
	 * [Job 1]: 참여자가 0명인 채팅방 삭제 (빈 방)
	 * 스케줄: 1분마다
	 * (방 생성 직후 0명인 찰나의 순간을 보호하기 위해, 생성 후 1분이 지난 방만 대상으로 함)
	 * 채팅방 생성 트랜잭션과 방장 Join 트랜잭션은 밀리초 단위로 발생하기 때문에 1분으로 설정함
	 * 근데 배치 작업 때문에, 추후 삭제할 채팅방이 많아진다면 1분으로 부족할 수도 있어서, 고려해볼 사항임
	 */
	// @Transactional
	public void cleanupEmptyRooms() {
		log.info("CleanupEmptyRoomsJob 시작");

		// KST(Asia/Seoul) 시간대 기준 현재 시각
		ZoneId seoulZone = ZoneId.of("Asia/Seoul");
		LocalDateTime nowKst = ZonedDateTime.now(seoulZone).toLocalDateTime();
		
		// KST 기준으로 1분 전 시간 계산 -> DB의 KST 값과 비교
		LocalDateTime graceTime = nowKst.minusMinutes(1);

		// 1. ID 조회
		List<String> emptyRoomIds = chatRoomRepository.findEmptyRoomIds(graceTime);
		log.info("emptyRoomIds.size() = {}", emptyRoomIds.size());

		if (!emptyRoomIds.isEmpty()) {
			// 2. PG DB 삭제 (ON DELETE CASCADE로 ChatRoomUser 동시 삭제)
			// 해당 JPQL 메서드가 내부적으로 @Transactional을 가짐
			long deletedCount = chatRoomRepository.deleteEmptyChatRoomsInPgTx(emptyRoomIds);
			log.info("[삭제] 참여자 0명인 채팅방 {}개 삭제됨", deletedCount);

			// 3. Redis/Mongo 정리
			chatRoomCleanupService.cleanupDeletedRoomData(emptyRoomIds);
		}
	}

	/**
	 * [Job 2]: 마지막 대화가 24시간 지난 채팅방 삭제
	 * 스케줄: 6시간마다
	 */
	@Scheduled(cron = "0 0 0/6 * * ?") // 0시, 6시, 12시, 18시에 실행
	// @Transactional
	public void cleanupInactiveRooms() {
		log.info("CleanupInactiveRoomsJob 시작");
		LocalDateTime now = LocalDateTime.now();
		LocalDateTime inactiveCutoff = now.minusHours(24);

		// 1. ID 조회
		List<String> inactiveRoomIds = chatRoomRepository.findInactiveRoomIds(inactiveCutoff);

		if (!inactiveRoomIds.isEmpty()) {
			// 2. PG DB 삭제
			long deletedCount = chatRoomRepository.deleteEmptyChatRoomsInPgTx(inactiveRoomIds);
			log.info("[삭제] 24시간 이상 대화 없는 채팅방 {}개 삭제됨", deletedCount);

			// 3. Redis/Mongo 정리
			chatRoomCleanupService.cleanupDeletedRoomData(inactiveRoomIds);
		}
	}

	/**
	 * [Job 3]: 축제 기간이 종료된 채팅방 삭제
	 * 스케줄: 일 1회
	 * (오늘 날짜 기준, 어제 종료된 축제까지 삭제)
	 * FestivalDataSyncJob의 의존성이므로 별도 Job으로 분리하지 않고 배치 Tasklet에서 호출
	 *
	 * 참고: FestivalDataSyncJob에서 Festival이 삭제되면 -> ChatRoom -> ChatRoomUser까지 CASCADE되지만,
	 * Redis/Mongo 정리를 위해 ID를 알아야 함. 따라서 Festival 삭제 전에 ChatRoom ID를 미리 조회 후 cleanupDeletedRoomData() 호출
	 */
	@Transactional
	public void cleanupEndedFestivalRooms() {
		log.info("CleanupEndedFestivalRoomsJob 시작");

		// 삭제 대상 ChatRoom ID를 먼저 조회 (Festival 삭제 전에 해야 함)
		LocalDate today = LocalDate.now();
		List<String> endedFestivalRoomIds = chatRoomRepository.findRoomsByFestivalEnded(today);

		if (!endedFestivalRoomIds.isEmpty()) {
			// PG 삭제는 FestivalDataSyncJob에 맡기고 Redis/Mongo 정리만
			chatRoomCleanupService.cleanupDeletedRoomData(endedFestivalRoomIds);
			log.warn("[삭제] 축제 종료 채팅방 {}개 Redis/Mongo 정리 완료.", endedFestivalRoomIds.size());
		}
	}
}
