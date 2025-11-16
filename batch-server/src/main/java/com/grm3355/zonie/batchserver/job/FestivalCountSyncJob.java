package com.grm3355.zonie.batchserver.job;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.grm3355.zonie.commonlib.domain.festival.repository.FestivalRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class FestivalCountSyncJob {

	private final FestivalRepository festivalRepository;

	/**
	 * 10분마다 실행
	 * Redis -> DB로 동기화된 채팅방(chat_rooms)의 참여자 수를
	 * 축제(festivals) 단위로 합산하여 total_participant_count 필드에 업데이트합니다.
	 */
	@Scheduled(cron = "0 */10 * * * *") // 매 10분마다 (0초, 10분 0초, 20분 0초...)
	public void syncFestivalParticipantCounts() {
		log.info("FestivalCountSyncJob 시작: 축제별 총 참여자 수 집계를 시작합니다.");
		try {
			festivalRepository.syncTotalParticipantCounts();
			log.info("FestivalCountSyncJob 완료: festivals.total_participant_count 업데이트 성공");
		} catch (Exception e) {
			log.error("축제별 총 참여자 수 집계 중 오류 발생", e);
		}
	}
}
