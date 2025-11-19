package com.grm3355.zonie.batchserver.job;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.grm3355.zonie.commonlib.domain.chatroom.repository.ChatRoomRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatRoomDeletionJob {

	private final ChatRoomRepository chatRoomRepository;

	/**
	 * 배치/스케줄러에 의해 호출될 메인 메소드
	 */
	@Transactional
	public void executeDeletionLogic() {
		log.info("ChatRoomDeletionJob 시작: 삭제 조건에 해당하는 채팅방을 정리합니다.");

		LocalDateTime now = LocalDateTime.now();

		// 1. 마지막 대화가 24시간 지난 채팅방 삭제
		LocalDateTime inactiveCutoff = now.minusHours(24);
		int deletedInactive = chatRoomRepository.deleteByLastMessageAtBefore(inactiveCutoff);
		if (deletedInactive > 0) {
			log.info("[삭제] 24시간 이상 대화 없는 채팅방 {}개 삭제됨", deletedInactive);
		}

		// 2. 참여자가 0명인 채팅방 삭제
		// (방 생성 직후 0명인 찰나의 순간을 보호하기 위해, 생성 후 3시간이 지난 방만 대상으로 함)
		LocalDateTime graceTime = now.minusHours(3);
		int deletedEmpty = chatRoomRepository.deleteEmptyRooms(graceTime);
		if (deletedEmpty > 0) {
			log.info("[삭제] 참여자가 0명인 채팅방 {}개 삭제됨", deletedEmpty);
		}

		// 3. 축제 기간이 종료된 채팅방 삭제
		// (오늘 날짜 기준, 어제 종료된 축제까지 삭제)
		LocalDate today = LocalDate.now();
		int deletedEndedFestivalRooms = chatRoomRepository.deleteByFestivalEnded(today);
		if (deletedEndedFestivalRooms > 0) {
			log.info("[삭제] 축제 종료된 채팅방 {}개 삭제됨", deletedEndedFestivalRooms);
		}

		log.info("ChatRoomDeletionJob 완료.");
	}
}
